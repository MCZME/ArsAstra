package com.github.mczme.arsastra.core.element.profile;

import com.github.mczme.arsastra.ArsAstra;
import com.github.mczme.arsastra.core.element.Element;
import com.github.mczme.arsastra.core.element.profile.definition.*;
import com.github.mczme.arsastra.registry.AARegistries;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import org.joml.Vector2f;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

public class ElementProfileManager extends SimpleJsonResourceReloadListener {

    private static final String FOLDER_NAME = "element_profiles";
    private static final ElementProfileManager INSTANCE = new ElementProfileManager();

    private Map<ResourceLocation, ElementProfile> profiles = new HashMap<>();
    private List<DefinitionFile> loadedDefinitions = new ArrayList<>();

    public ElementProfileManager() {
        super(new Gson(), FOLDER_NAME);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        ArsAstra.LOGGER.info("Loading element profiles from data packs (V2)...");
        List<DefinitionFile> definitions = new ArrayList<>();
        // 1. 加载阶段
        for (Map.Entry<ResourceLocation, JsonElement> entry : pObject.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            JsonElement json = entry.getValue();
            try {
                DefinitionFile.CODEC.parse(JsonOps.INSTANCE, json)
                        .resultOrPartial(error -> ArsAstra.LOGGER.error("Failed to parse element definition file {}: {}", fileId, error))
                        .ifPresent(definitions::add);
            } catch (Exception e) {
                ArsAstra.LOGGER.error("Failed to load element definition file: " + fileId, e);
            }
        }
        this.loadedDefinitions = definitions;
        ArsAstra.LOGGER.info("Found {} element definition files. Processing will occur after tags are loaded.", definitions.size());
    }

    public void processDefinitions(long seed) {
        ArsAstra.LOGGER.debug("Processing {} element definition files with custom seed {}...", loadedDefinitions.size(), seed);
        Random random = new Random(seed);

        // 2. 排序阶段
        List<DefinitionFile> definitions = new ArrayList<>(this.loadedDefinitions);
        definitions.sort(Comparator.comparingInt(DefinitionFile::priority));

        // 3. 处理阶段
        Map<ResourceLocation, WorkInProgressProfile> wipProfiles = new HashMap<>();

        for (DefinitionFile defFile : definitions) {
            var definition = defFile.definition();
            switch (definition.type()) {
                case RANDOM -> {
                    if (definition instanceof RandomDefinition randomDef) {
                        randomDef.apply().forEach(applyEntry -> streamTargets(applyEntry).forEach(itemId -> {
                            // 仅当物品尚未被定义时才应用随机配置（最低优先级）
                            if (!wipProfiles.containsKey(itemId)) {
                                var wip = wipProfiles.computeIfAbsent(itemId, k -> new WorkInProgressProfile());
                                generateRandomProfile(randomDef.rules(), random).ifPresent(wip::applyPartial);
                            }
                        }));
                    }
                }
                case TEMPLATE -> {
                    if (definition instanceof TemplateDefinition templateDef) {
                        templateDef.apply().forEach(applyEntry -> {
                            var sharedData = templateDef.data().orElse(null);
                            streamTargets(applyEntry).forEach(itemId -> {
                                var wip = wipProfiles.computeIfAbsent(itemId, k -> new WorkInProgressProfile());
                                if (sharedData != null) {
                                    sharedData.launchPoint().ifPresent(wip::setLaunchPoint);
                                    sharedData.elements().ifPresent(wip::addElements);
                                }
                                applyEntry.launchPoint().ifPresent(wip::setLaunchPoint); // 覆盖发射点
                                wip.addElements(applyEntry.elements());
                            });
                        });
                    }
                }
                case BASIC -> {
                    if (definition instanceof BasicDefinition basicDef) {
                        basicDef.profiles().forEach(entry -> {
                            var wip = wipProfiles.computeIfAbsent(entry.item(), k -> new WorkInProgressProfile());
                            wip.setLaunchPoint(entry.profile().launchPoint());
                            wip.setElements(entry.profile().elements());
                        });
                    }
                }
            }
        }

        // 4. 最终化阶段
        Map<ResourceLocation, ElementProfile> finalProfiles = new HashMap<>();
        for (var entry : wipProfiles.entrySet()) {
            entry.getValue().build(entry.getKey()).ifPresent(profile -> finalProfiles.put(entry.getKey(), profile));
        }
        this.profiles = finalProfiles;

        ArsAstra.LOGGER.info("Finished processing element profiles. {} final profiles generated.", profiles.size());
    }
                
    private Stream<ResourceLocation> streamTargets(TemplateApplyEntry entry) {
        if (entry.item().isPresent()) return Stream.of(entry.item().get());
        if (entry.tag().isPresent()) {
            TagKey<Item> tagKey = TagKey.create(Registries.ITEM, entry.tag().get());
            return BuiltInRegistries.ITEM.getTag(tagKey)
                    .map(holders -> holders.stream().map(Holder::getKey))
                    .orElse(Stream.empty())
                    .map(ResourceKey::location); // 将 ResourceKey 映射到 ResourceLocation
        }
        return Stream.empty();
    }

    private Stream<ResourceLocation> streamTargets(RandomApplyEntry entry) {
        if (entry.item().isPresent()) return Stream.of(entry.item().get());
        if (entry.tag().isPresent()) {
            TagKey<Item> tagKey = TagKey.create(Registries.ITEM, entry.tag().get());
            return BuiltInRegistries.ITEM.getTag(tagKey)
                    .map(holders -> holders.stream().map(Holder::getKey))
                    .orElse(Stream.empty())
                    .map(ResourceKey::location);
        }
        return Stream.empty();
    }

    private Optional<PartialProfile> generateRandomProfile(RandomRules rules, Random random) {
        // 生成要素
        Map<ResourceLocation, Float> generatedElements = new HashMap<>();
        rules.totalValueRange().ifPresent(range -> {
            if (range.size() != 2)
                return;
            float totalValue = random.nextFloat(range.get(0), range.get(1));
            int maxTypes = rules.maxElementTypes().orElse(1);
            List<ElementPoolEntry> pool = new ArrayList<>(rules.pool().orElse(List.of()));
            if (pool.isEmpty())
                return;

            Collections.shuffle(pool, random);
            int typesToGenerate = random.nextInt(1, Math.min(pool.size(), maxTypes) + 1);
            List<ElementPoolEntry> chosenPool = pool.subList(0, typesToGenerate);

            float totalWeight = (float) chosenPool.stream().mapToDouble(ElementPoolEntry::weight).sum();
            float remainingValue = totalValue;
            for (int i = 0; i < chosenPool.size() - 1; i++) {
                ElementPoolEntry entry = chosenPool.get(i);
                float value = totalValue * (entry.weight() / totalWeight)
                        * (random.nextFloat(0.5f, 1.5f));
                value = Math.min(value, remainingValue);
                generatedElements.put(entry.element(), value);
                remainingValue -= value;
            }
            generatedElements.put(chosenPool.getLast().element(), remainingValue);
        });

        if (generatedElements.isEmpty()) {
            return Optional.empty();
        }

        // 计算初始点
        Vector2f launchPoint = calculateLaunchPoint(generatedElements);

        return Optional
                .of(new PartialProfile(Optional.of(launchPoint), Optional.of(generatedElements)));
    }
                    
    private Vector2f calculateLaunchPoint(Map<ResourceLocation, Float> elements) {
        Vector2f finalVector = new Vector2f(0.0f, 0.0f);
        float totalValue = 0.0f;
        for (Map.Entry<ResourceLocation, Float> entry : elements.entrySet()) {
            var elementOpt = AARegistries.ELEMENT_REGISTRY.getOptional(entry.getKey());
            if (elementOpt.isPresent()) {
                Element element = elementOpt.get();
                float value = entry.getValue();
                finalVector.add(new Vector2f(element.getVector()).mul(value));
                totalValue += value;
            }
        }
        if (totalValue > 0) finalVector.div(totalValue);
        return finalVector;
    }


    public static ElementProfileManager getInstance() {
        return INSTANCE;
    }

    public Optional<ElementProfile> getElementProfile(Item item) {
        return Optional.ofNullable(profiles.get(BuiltInRegistries.ITEM.getKey(item)));
    }

    private static class WorkInProgressProfile {
        private Vector2f launchPoint;
        private final Map<ResourceLocation, Float> elements = new HashMap<>();

        public void applyPartial(PartialProfile partial) {
            partial.launchPoint().ifPresent(this::setLaunchPoint);
            partial.elements().ifPresent(this::addElements);
        }

        public void setLaunchPoint(Vector2f launchPoint) {
            this.launchPoint = launchPoint;
        }

        public void addElements(Map<ResourceLocation, Float> toAdd) {
            this.elements.putAll(toAdd);
        }

        public void setElements(Map<ResourceLocation, Float> newElements) {
            this.elements.clear();
            this.elements.putAll(newElements);
        }

        public Optional<ElementProfile> build(ResourceLocation itemId) {
            if (this.launchPoint == null) {
                // ArsAstra.LOGGER.warn("因 'launch_point' 缺失，跳过物品 {} 的配置。", itemId);
                return Optional.empty();
            }
            // 使用设计中指定的必须包含 launch_point 的构造函数
            return Optional.of(new ElementProfile(this.launchPoint, new HashMap<>(this.elements)));
        }
    }
}

