package com.github.mczme.arsastra.data.provider;

import com.github.mczme.arsastra.ArsAstra;
import com.github.mczme.arsastra.registry.AABlocks;
import com.github.mczme.arsastra.registry.AATags;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class AABlockTagsProvider extends BlockTagsProvider {
    public AABlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, ArsAstra.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider pProvider) {
        tag(BlockTags.MINEABLE_WITH_AXE)
                .add(AABlocks.ANALYSIS_DESK.get());

        tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(AABlocks.COPPER_TUN.get());

        tag(BlockTags.NEEDS_STONE_TOOL)
                .add(AABlocks.COPPER_TUN.get());

        tag(AATags.Blocks.HEAT_SOURCES)
                .addTag(BlockTags.CAMPFIRES)
                .addTag(BlockTags.FIRE)
                .add(Blocks.LAVA)
                .add(Blocks.MAGMA_BLOCK);
    }
}
