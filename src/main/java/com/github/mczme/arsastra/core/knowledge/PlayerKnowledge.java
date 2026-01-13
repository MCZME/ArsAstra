package com.github.mczme.arsastra.core.knowledge;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class PlayerKnowledge implements INBTSerializable<CompoundTag> {
    // 存储已探索的星图 ID
    private final Set<ResourceLocation> visitedStarCharts = new HashSet<>();
    // 存储已分析的物品 ID
    private final Set<ResourceLocation> analyzedItems = new HashSet<>();
    // 存储已识别的效果 ID (MobEffect)
    private final Set<ResourceLocation> knownEffects = new HashSet<>();

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
     * 判断是否已识别某效果
     */
    public boolean hasKnownEffect(MobEffect effect) {
        return knownEffects.contains(BuiltInRegistries.MOB_EFFECT.getKey(effect));
    }

    /**
     * 学习/识别效果
     * @return 如果是第一次识别，返回 true
     */
    public boolean learnEffect(MobEffect effect) {
        return knownEffects.add(BuiltInRegistries.MOB_EFFECT.getKey(effect));
    }

    public Set<ResourceLocation> getKnownEffects() {
        return Collections.unmodifiableSet(knownEffects);
    }

    /**
     * 清空所有知识
     */
    public void clear() {
        visitedStarCharts.clear();
        analyzedItems.clear();
        knownEffects.clear();
    }

    /**
     * 用于玩家重生时复制数据
     */
    public void copyFrom(PlayerKnowledge other) {
        this.visitedStarCharts.clear();
        this.visitedStarCharts.addAll(other.visitedStarCharts);

        this.analyzedItems.clear();
        this.analyzedItems.addAll(other.analyzedItems);

        this.knownEffects.clear();
        this.knownEffects.addAll(other.knownEffects);
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

        ListTag effectsTag = new ListTag();
        for (ResourceLocation id : knownEffects) {
            effectsTag.add(StringTag.valueOf(id.toString()));
        }
        tag.put("KnownEffects", effectsTag);

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

        knownEffects.clear();
        if (tag.contains("KnownEffects", Tag.TAG_LIST)) {
            ListTag list = tag.getList("KnownEffects", Tag.TAG_STRING);
            for (Tag t : list) {
                knownEffects.add(ResourceLocation.parse(t.getAsString()));
            }
        }
    }
}
