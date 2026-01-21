package com.github.mczme.arsastra.registry;

import com.github.mczme.arsastra.ArsAstra;
import com.github.mczme.arsastra.block.entity.AnalysisDeskBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;

public class AABlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ArsAstra.MODID);

    public static final Supplier<BlockEntityType<AnalysisDeskBlockEntity>> ANALYSIS_DESK =
            BLOCK_ENTITIES.register("analysis_desk",
                    () -> BlockEntityType.Builder.of(AnalysisDeskBlockEntity::new, AABlocks.ANALYSIS_DESK.get()).build(null));

    public static final Supplier<BlockEntityType<com.github.mczme.arsastra.block.entity.CopperTunBlockEntity>> COPPER_TUN =
            BLOCK_ENTITIES.register("copper_tun",
                    () -> BlockEntityType.Builder.of(com.github.mczme.arsastra.block.entity.CopperTunBlockEntity::new, AABlocks.COPPER_TUN.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
