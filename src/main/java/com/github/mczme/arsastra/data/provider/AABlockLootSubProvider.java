package com.github.mczme.arsastra.data.provider;

import com.github.mczme.arsastra.registry.AABlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;

import java.util.Collections;

public class AABlockLootSubProvider extends BlockLootSubProvider {
    public AABlockLootSubProvider(HolderLookup.Provider provider) {
        super(Collections.emptySet(), FeatureFlags.REGISTRY.allFlags(), provider);
    }

    @Override
    protected void generate() {
        dropSelf(AABlocks.ANALYSIS_DESK.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return AABlocks.BLOCKS.getEntries().stream().map(block -> (Block) block.get()).toList();
    }
}
