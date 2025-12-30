package com.github.mczme.arsastra.registry;

import com.github.mczme.arsastra.ArsAstra;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class AABlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ArsAstra.MODID);

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
