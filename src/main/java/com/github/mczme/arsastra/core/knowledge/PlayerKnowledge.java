package com.github.mczme.arsastra.core.knowledge;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.HashSet;
import java.util.Set;

public class PlayerKnowledge implements INBTSerializable<CompoundTag> {
    // 存储已解锁的要素 ID
    private final Set<ResourceLocation> unlockedElements = new HashSet<>();
    // 存储已探索的星图 ID
    private final Set<ResourceLocation> visitedStarCharts = new HashSet<>();
    // 存储已分析的物品 ID
    private final Set<ResourceLocation> analyzedItems = new HashSet<>();

    public PlayerKnowledge() {
    }

    /**
     * 判断是否已解锁某要素
     */
    public boolean hasUnlockedElement(ResourceLocation elementId) {
        return unlockedElements.contains(elementId);
    }

    /**
     * 解锁新要素
     * @return 如果是新解锁的，返回 true
     */
    public boolean unlockElement(ResourceLocation elementId) {
        return unlockedElements.add(elementId);
    }

    public Set<ResourceLocation> getUnlockedElements() {
        return new HashSet<>(unlockedElements);
    }

    public boolean hasVisitedStarChart(ResourceLocation starChartId) {
        return visitedStarCharts.contains(starChartId);
    }

    public boolean visitStarChart(ResourceLocation starChartId) {
        return visitedStarCharts.add(starChartId);
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
     * 用于玩家重生时复制数据
     */
    public void copyFrom(PlayerKnowledge other) {
        this.unlockedElements.clear();
        this.unlockedElements.addAll(other.unlockedElements);
        
        this.visitedStarCharts.clear();
        this.visitedStarCharts.addAll(other.visitedStarCharts);

        this.analyzedItems.clear();
        this.analyzedItems.addAll(other.analyzedItems);
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        
        ListTag elementsTag = new ListTag();
        for (ResourceLocation id : unlockedElements) {
            elementsTag.add(StringTag.valueOf(id.toString()));
        }
        tag.put("UnlockedElements", elementsTag);

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

        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        unlockedElements.clear();
        if (tag.contains("UnlockedElements", Tag.TAG_LIST)) {
            ListTag list = tag.getList("UnlockedElements", Tag.TAG_STRING);
            for (Tag t : list) {
                unlockedElements.add(ResourceLocation.parse(t.getAsString()));
            }
        }

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
    }
}
