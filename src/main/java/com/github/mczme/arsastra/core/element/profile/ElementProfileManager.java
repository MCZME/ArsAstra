package com.github.mczme.arsastra.core.element.profile;

import com.github.mczme.arsastra.ArsAstra;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ElementProfileManager extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String FOLDER_NAME = "item_profiles";
    private static final ElementProfileManager INSTANCE = new ElementProfileManager();

    private Map<ResourceLocation, ElementProfile> profiles = new HashMap<>();

    public ElementProfileManager() {
        super(GSON, FOLDER_NAME);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        ArsAstra.LOGGER.info("Loading element profiles from data packs...");
        Map<ResourceLocation, ElementProfile> newProfiles = new HashMap<>();

        for (Map.Entry<ResourceLocation, JsonElement> entry : pObject.entrySet()) {
            ResourceLocation fileLocation = entry.getKey();
            if (!entry.getValue().isJsonObject()) continue;
            JsonObject json = entry.getValue().getAsJsonObject();

            try {
                if (!json.has("item")) {
                    ArsAstra.LOGGER.error("Element profile file {} is missing 'item' field.", fileLocation);
                    continue;
                }
                ResourceLocation itemId = ResourceLocation.parse(json.get("item").getAsString());

                ElementProfile.CODEC.parse(JsonOps.INSTANCE, json)
                        .resultOrPartial(error -> ArsAstra.LOGGER.error("Failed to parse element profile for {}: {}", itemId, error))
                        .ifPresent(profile -> {
                            newProfiles.put(itemId, profile);
                            ArsAstra.LOGGER.debug("Loaded element profile for item: {}", itemId);
                        });
            } catch (Exception e) {
                ArsAstra.LOGGER.error("Failed to load element profile file: " + fileLocation, e);
            }
        }

        this.profiles = newProfiles;
        ArsAstra.LOGGER.info("Finished loading {} element profiles.", profiles.size());
    }

    public static ElementProfileManager getInstance() {
        return INSTANCE;
    }

    public Optional<ElementProfile> getElementProfile(Item item) {
        return Optional.ofNullable(profiles.get(BuiltInRegistries.ITEM.getKey(item)));
    }
}
