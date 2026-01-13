package com.github.mczme.arsastra.core.knowledge;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.*;

public class PlayerKnowledge implements INBTSerializable<CompoundTag> {
    // 存储已探索的星图 ID
    private final Set<ResourceLocation> visitedStarCharts = new HashSet<>();
    // 存储已分析的物品 ID
    private final Set<ResourceLocation> analyzedItems = new HashSet<>();
    // 存储已识别的效果星域 (星图ID -> 索引集合)
    private final Map<ResourceLocation, Set<Integer>> knownFields = new HashMap<>();

    public PlayerKnowledge() {
    }

    public boolean hasVisitedStarChart(ResourceLocation starChartId) {
        return visitedStarCharts.contains(starChartId);
    }

    public Set<ResourceLocation> getVisitedStarCharts() {
        return Collections.unmodifiableSet(visitedStarCharts);
    }

    public boolean visitStarChart(ResourceLocation starChartId) {
        return visitedStarCharts.add(starChartId);
    }

    public Set<ResourceLocation> getAnalyzedItems() {
        return Collections.unmodifiableSet(analyzedItems);
    }

    /**
     * 判断是否已分析某物品
     */
    public boolean hasAnalyzed(Item item) {
        return analyzedItems.contains(BuiltInRegistries.ITEM.getKey(item));
    }

    /**
     * 分析物品
     * @return 如果是第一次分析，返回 true
     */
    public boolean analyzeItem(Item item) {
        return analyzedItems.add(BuiltInRegistries.ITEM.getKey(item));
    }

    /**
     * 判断某星图的特定效果星域是否已解锁
     */
    public boolean hasUnlockedField(ResourceLocation chartId, int fieldIndex) {
        return knownFields.containsKey(chartId) && knownFields.get(chartId).contains(fieldIndex);
    }

    /**
     * 解锁某星图的特定效果星域
     * @return 如果是第一次解锁，返回 true
     */
    public boolean unlockField(ResourceLocation chartId, int fieldIndex) {
        return knownFields.computeIfAbsent(chartId, k -> new HashSet<>()).add(fieldIndex);
    }

    public Set<Integer> getUnlockedFields(ResourceLocation chartId) {
        return knownFields.getOrDefault(chartId, Collections.emptySet());
    }
    
    public Map<ResourceLocation, Set<Integer>> getAllKnownFields() {
        return Collections.unmodifiableMap(knownFields);
    }

    /**
     * 清空所有知识
     */
    public void clear() {
        visitedStarCharts.clear();
        analyzedItems.clear();
        knownFields.clear();
    }

    /**
     * 用于玩家重生时复制数据
     */
    public void copyFrom(PlayerKnowledge other) {
        this.visitedStarCharts.clear();
        this.visitedStarCharts.addAll(other.visitedStarCharts);

        this.analyzedItems.clear();
        this.analyzedItems.addAll(other.analyzedItems);

        this.knownFields.clear();
        other.knownFields.forEach((k, v) -> this.knownFields.put(k, new HashSet<>(v)));
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        
        ListTag chartsTag = new ListTag();
        for (ResourceLocation id : visitedStarCharts) {
            chartsTag.add(StringTag.valueOf(id.toString()));
        }
        tag.put("VisitedStarCharts", chartsTag);

        ListTag itemsTag = new ListTag();
        for (ResourceLocation id : analyzedItems) {
            itemsTag.add(StringTag.valueOf(id.toString()));
        }
        tag.put("AnalyzedItems", itemsTag);

        CompoundTag fieldsTag = new CompoundTag();
        for (Map.Entry<ResourceLocation, Set<Integer>> entry : knownFields.entrySet()) {
            List<Integer> list = new ArrayList<>(entry.getValue());
            fieldsTag.put(entry.getKey().toString(), new IntArrayTag(list));
        }
        tag.put("KnownFields", fieldsTag);

        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        visitedStarCharts.clear();
        if (tag.contains("VisitedStarCharts", Tag.TAG_LIST)) {
            ListTag list = tag.getList("VisitedStarCharts", Tag.TAG_STRING);
            for (Tag t : list) {
                visitedStarCharts.add(ResourceLocation.parse(t.getAsString()));
            }
        }

        analyzedItems.clear();
        if (tag.contains("AnalyzedItems", Tag.TAG_LIST)) {
            ListTag list = tag.getList("AnalyzedItems", Tag.TAG_STRING);
            for (Tag t : list) {
                analyzedItems.add(ResourceLocation.parse(t.getAsString()));
            }
        }

        knownFields.clear();
        if (tag.contains("KnownFields", Tag.TAG_COMPOUND)) {
            CompoundTag fieldsTag = tag.getCompound("KnownFields");
            for (String key : fieldsTag.getAllKeys()) {
                ResourceLocation chartId = ResourceLocation.parse(key);
                int[] indices = fieldsTag.getIntArray(key);
                Set<Integer> set = new HashSet<>();
                for (int i : indices) set.add(i);
                knownFields.put(chartId, set);
            }
        }
    }
}
