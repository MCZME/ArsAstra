package com.github.mczme.arsastra.data.provider;

import com.github.mczme.arsastra.ArsAstra;
import com.github.mczme.arsastra.core.element.Element;
import com.github.mczme.arsastra.core.element.profile.ElementProfile;
import com.github.mczme.arsastra.core.element.profile.definition.*;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.JsonCodecProvider;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public abstract class ElementProfileProvider extends JsonCodecProvider<DefinitionFile> {

    public ElementProfileProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, ExistingFileHelper existingFileHelper) {
        super(output, PackOutput.Target.DATA_PACK, "element_profiles", PackType.SERVER_DATA, DefinitionFile.CODEC, lookupProvider, ArsAstra.MODID, existingFileHelper);
    }

    protected void basic(String name, int priority, BasicDefinitionBuilder builder) {
        unconditional(ResourceLocation.fromNamespaceAndPath(ArsAstra.MODID, name), new DefinitionFile(priority, builder.build()));
    }

    protected void template(String name, int priority, TemplateDefinitionBuilder builder) {
        unconditional(ResourceLocation.fromNamespaceAndPath(ArsAstra.MODID, name), new DefinitionFile(priority, builder.build()));
    }

    protected void random(String name, int priority, RandomDefinitionBuilder builder) {
        unconditional(ResourceLocation.fromNamespaceAndPath(ArsAstra.MODID, name), new DefinitionFile(priority, builder.build()));
    }

    protected BasicDefinitionBuilder basicBuilder() {
        return new BasicDefinitionBuilder();
    }

    protected TemplateDefinitionBuilder templateBuilder() {
        return new TemplateDefinitionBuilder();
    }

    protected RandomDefinitionBuilder randomBuilder() {
        return new RandomDefinitionBuilder();
    }

    public static class BasicDefinitionBuilder {
        private final List<BasicProfileEntry> entries = new ArrayList<>();

        public BasicDefinitionBuilder add(ResourceLocation item, ElementProfile profile) {
            entries.add(new BasicProfileEntry(item, profile));
            return this;
        }

        public BasicDefinitionBuilder add(ItemLike item, ElementProfile profile) {
            return add(BuiltInRegistries.ITEM.getKey(item.asItem()), profile);
        }

        public BasicDefinitionBuilder add(ResourceLocation item, Vector2f launchPoint, Map<ResourceLocation, Float> elements) {
            return add(item, new ElementProfile(launchPoint, elements));
        }

        public BasicDefinitionBuilder add(ItemLike item, Vector2f launchPoint, Map<? extends Holder<Element>, Float> elements) {
            Map<ResourceLocation, Float> map = new HashMap<>();
            elements.forEach((holder, value) -> map.put(holder.getKey().location(), value));
            return add(BuiltInRegistries.ITEM.getKey(item.asItem()), launchPoint, map);
        }

        public BasicDefinition build() {
            return new BasicDefinition(entries);
        }
    }

    public static class TemplateDefinitionBuilder {
        private PartialProfile data = null;
        private final List<TemplateApplyEntry> apply = new ArrayList<>();

        public TemplateDefinitionBuilder data(PartialProfile data) {
            this.data = data;
            return this;
        }

        public TemplateDefinitionBuilder data(Vector2f launchPoint, Map<? extends Holder<Element>, Float> elements) {
            Map<ResourceLocation, Float> map = new HashMap<>();
            elements.forEach((holder, value) -> map.put(holder.getKey().location(), value));
            return data(new PartialProfile(Optional.ofNullable(launchPoint), Optional.of(map)));
        }

        public TemplateDefinitionBuilder apply(TemplateApplyEntry entry) {
            this.apply.add(entry);
            return this;
        }

        public TemplateDefinitionBuilder apply(ItemLike item, Vector2f launchPoint, Map<? extends Holder<Element>, Float> elements) {
            Map<ResourceLocation, Float> map = new HashMap<>();
            elements.forEach((holder, value) -> map.put(holder.getKey().location(), value));
            return apply(new TemplateApplyEntry(
                    Optional.of(BuiltInRegistries.ITEM.getKey(item.asItem())),
                    Optional.empty(),
                    Optional.ofNullable(launchPoint),
                    map
            ));
        }

        public TemplateDefinitionBuilder apply(TagKey<Item> tag, Vector2f launchPoint, Map<? extends Holder<Element>, Float> elements) {
            Map<ResourceLocation, Float> map = new HashMap<>();
            elements.forEach((holder, value) -> map.put(holder.getKey().location(), value));
            return apply(new TemplateApplyEntry(
                    Optional.empty(),
                    Optional.of(tag.location()),
                    Optional.ofNullable(launchPoint),
                    map
            ));
        }

        public TemplateDefinition build() {
            return new TemplateDefinition(Optional.ofNullable(data), apply);
        }
    }

    public static class RandomDefinitionBuilder {
        private final List<RandomApplyEntry> apply = new ArrayList<>();
        private RandomRules rules;

        public RandomDefinitionBuilder apply(RandomApplyEntry entry) {
            this.apply.add(entry);
            return this;
        }

        public RandomDefinitionBuilder apply(ItemLike item) {
            return apply(new RandomApplyEntry(
                    Optional.of(BuiltInRegistries.ITEM.getKey(item.asItem())),
                    Optional.empty()
            ));
        }

        public RandomDefinitionBuilder apply(TagKey<Item> tag) {
            return apply(new RandomApplyEntry(
                    Optional.empty(),
                    Optional.of(tag.location())
            ));
        }

        public RandomDefinitionBuilder rules(RandomRules rules) {
            this.rules = rules;
            return this;
        }

        public RandomDefinition build() {
            return new RandomDefinition(apply, rules);
        }
    }
}
