package com.github.mczme.arsastra.registry;

import com.github.mczme.arsastra.ArsAstra;
import com.github.mczme.arsastra.core.starchart.environment.EnvironmentType;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class AAEnvironmentTypes {
    public static final DeferredRegister<EnvironmentType> ENVIRONMENT_TYPES = DeferredRegister.create(AARegistries.ENVIRONMENT_TYPE_REGISTRY_KEY, ArsAstra.MODID);

    public static void register(IEventBus eventBus) {
        ENVIRONMENT_TYPES.register(eventBus);
    }
}
