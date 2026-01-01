package com.github.mczme.arsastra.registry;

import com.github.mczme.arsastra.ArsAstra;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class AAComponents {
    public static final DeferredRegister<DataComponentType<?>> REGISTRAR = DeferredRegister
            .create(Registries.DATA_COMPONENT_TYPE, ArsAstra.MODID);

    public static void register(IEventBus modEventBus) {
        REGISTRAR.register(modEventBus);
    }
}

