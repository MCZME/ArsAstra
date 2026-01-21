package com.github.mczme.arsastra.data.provider;

import com.github.mczme.arsastra.ArsAstra;
import com.github.mczme.arsastra.registry.AABlocks;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class AABlockStateProvider extends BlockStateProvider {
    public AABlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, ArsAstra.MODID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        horizontalBlock(AABlocks.ANALYSIS_DESK.get(), models().getExistingFile(modLoc("block/analysis_desk")));

        simpleBlock(AABlocks.COPPER_TUN.get(), models().getBuilder("copper_tun")
                .texture("particle", "minecraft:block/copper_block"));
    }
}
