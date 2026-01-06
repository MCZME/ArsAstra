package com.github.mczme.arsastra.core.starchart;

import com.github.mczme.arsastra.ArsAstra;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class StarChartManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String FOLDER_NAME = "star_charts";
    private static final StarChartManager INSTANCE = new StarChartManager();

    private Map<ResourceLocation, StarChart> charts = new HashMap<>();

    public StarChartManager() {
        super(GSON, FOLDER_NAME);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        ArsAstra.LOGGER.info("Loading star charts from data packs...");
        Map<ResourceLocation, StarChart> newCharts = new HashMap<>();

        for (Map.Entry<ResourceLocation, JsonElement> entry : pObject.entrySet()) {
            ResourceLocation fileLocation = entry.getKey();
            try {
                StarChart.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                        .resultOrPartial(error -> ArsAstra.LOGGER.error("Failed to parse star chart {}: {}", fileLocation, error))
                        .ifPresent(chart -> {
                            newCharts.put(fileLocation, chart);
                            ArsAstra.LOGGER.debug("Loaded star chart: {}", fileLocation);
                        });
            } catch (Exception e) {
                ArsAstra.LOGGER.error("Failed to load star chart file: " + fileLocation, e);
            }
        }

        this.charts = newCharts;
        ArsAstra.LOGGER.info("Finished loading {} star charts.", charts.size());
    }

    public static StarChartManager getInstance() {
        return INSTANCE;
    }

    public Optional<StarChart> getStarChart(ResourceLocation id) {
        return Optional.ofNullable(charts.get(id));
    }

    public java.util.Set<ResourceLocation> getStarChartIds() {
        return charts.keySet();
    }

    public Map<ResourceLocation, StarChart> getStarCharts() {
        return charts;
    }
}
