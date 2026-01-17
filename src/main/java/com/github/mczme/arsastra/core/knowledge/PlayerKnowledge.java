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

/**
 * 玩家知识系统核心数据类。
 * 负责存储和管理玩家在星图探索过程中积累的知识，包括：
 * <ul>
 *     <li>已探索的星图 (Visited Star Charts)</li>
 *     <li>已分析的物品 (Analyzed Items)</li>
 *     <li>已识别的效果星域 (Known Effect Fields)</li>
 * </ul>
 * 此数据会作为 Capability 附加到玩家实体上，并支持 NBT 序列化。
 */
public class PlayerKnowledge implements INBTSerializable<CompoundTag> {
    /** 存储已探索的星图 ID */
    private final Set<ResourceLocation> visitedStarCharts = new HashSet<>();
    /** 存储已分析的物品 ID */
    private final Set<ResourceLocation> analyzedItems = new HashSet<>();
    /** 存储已识别的效果星域 (星图ID -> 索引集合) */
    private final Map<ResourceLocation, Set<Integer>> knownFields = new HashMap<>();

    public PlayerKnowledge() {
    }

    /**
     * 检查玩家是否已经访问过指定的星图。
     * @param starChartId 星图的资源 ID
     * @return 如果已访问返回 true，否则返回 false
     */
    public boolean hasVisitedStarChart(ResourceLocation starChartId) {
        return visitedStarCharts.contains(starChartId);
    }

    /**
     * 获取玩家所有已访问星图的不可变集合。
     * @return 已访问星图 ID 集合
     */
    public Set<ResourceLocation> getVisitedStarCharts() {
        return Collections.unmodifiableSet(visitedStarCharts);
    }

    /**
     * 标记星图为已访问。
     * @param starChartId 星图的资源 ID
     * @return 如果是首次访问返回 true，如果已存在则返回 false
     */
    public boolean visitStarChart(ResourceLocation starChartId) {
        return visitedStarCharts.add(starChartId);
    }

    /**
     * 获取玩家所有已分析物品的不可变集合。
     * @return 已分析物品 ID 集合
     */
    public Set<ResourceLocation> getAnalyzedItems() {
        return Collections.unmodifiableSet(analyzedItems);
    }

    /**
     * 判断是否已分析某物品。
     * @param item 待检查的物品
     * @return 如果已分析返回 true，否则返回 false
     */
    public boolean hasAnalyzed(Item item) {
        return analyzedItems.contains(BuiltInRegistries.ITEM.getKey(item));
    }

    /**
     * 分析物品，将其加入已知列表。
     * @param item 被分析的物品
     * @return 如果是第一次分析返回 true，如果已存在则返回 false
     */
    public boolean analyzeItem(Item item) {
        return analyzedItems.add(BuiltInRegistries.ITEM.getKey(item));
    }

    /**
     * 判断某星图的特定效果星域是否已解锁。
     * @param chartId 星图 ID
     * @param fieldIndex 效果星域在星图中的索引
     * @return 如果已解锁返回 true
     */
    public boolean hasUnlockedField(ResourceLocation chartId, int fieldIndex) {
        return knownFields.containsKey(chartId) && knownFields.get(chartId).contains(fieldIndex);
    }

    /**
     * 解锁某星图的特定效果星域。
     * @param chartId 星图 ID
     * @param fieldIndex 效果星域索引
     * @return 如果是第一次解锁返回 true
     */
    public boolean unlockField(ResourceLocation chartId, int fieldIndex) {
        return knownFields.computeIfAbsent(chartId, k -> new HashSet<>()).add(fieldIndex);
    }

    /**
     * 获取指定星图中已解锁的所有效果星域索引。
     * @param chartId 星图 ID
     * @return 索引集合
     */
    public Set<Integer> getUnlockedFields(ResourceLocation chartId) {
        return knownFields.getOrDefault(chartId, Collections.emptySet());
    }
    
    /**
     * 获取所有已知的效果星域数据。
     * @return 不可修改的 Map (星图ID -> 索引集合)
     */
    public Map<ResourceLocation, Set<Integer>> getAllKnownFields() {
        return Collections.unmodifiableMap(knownFields);
    }

    /**
     * 清空所有知识。
     */
    public void clear() {
        visitedStarCharts.clear();
        analyzedItems.clear();
        knownFields.clear();
    }

    /**
     * 用于玩家重生时复制数据。
     * @param other 旧的知识数据对象
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
