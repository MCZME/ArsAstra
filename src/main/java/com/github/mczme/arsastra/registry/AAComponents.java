package com.github.mczme.arsastra.registry;

import com.github.mczme.arsastra.ArsAstra;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.UnaryOperator;

public class AAComponents {
    public static final DeferredRegister<DataComponentType<?>> REGISTRAR = DeferredRegister
            .create(Registries.DATA_COMPONENT_TYPE, ArsAstra.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> IS_OPEN = register("is_open",
            builder -> builder.persistent(Codec.BOOL));

    private static <T> DeferredHolder<DataComponentType<?>, DataComponentType<T>> register(String name,
                                                                                           UnaryOperator<DataComponentType.Builder<T>> builderOperator) {
        return REGISTRAR.register(name, () -> builderOperator.apply(DataComponentType.builder()).build());
    }

    public static void register(IEventBus modEventBus) {
        REGISTRAR.register(modEventBus);
    }
}

