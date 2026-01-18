package com.github.mczme.arsastra.registry;

import com.github.mczme.arsastra.ArsAstra;
import com.github.mczme.arsastra.block.AnalysisDeskBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class AABlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ArsAstra.MODID);

    public static final DeferredBlock<AnalysisDeskBlock> ANALYSIS_DESK = BLOCKS.register("analysis_desk",
            () -> new AnalysisDeskBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).strength(2.5f).noOcclusion()));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
