package com.github.mczme.arsastra.registry;

import com.github.mczme.arsastra.ArsAstra;
import com.github.mczme.arsastra.core.starchart.environment.EnvironmentType;
import com.github.mczme.arsastra.core.starchart.environment.type.BorderEnvironmentType;
import com.github.mczme.arsastra.core.starchart.environment.type.InhibitorEnvironmentType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class AAEnvironmentTypes {
    public static final DeferredRegister<EnvironmentType> ENVIRONMENT_TYPES = DeferredRegister.create(AARegistries.ENVIRONMENT_TYPE_REGISTRY_KEY, ArsAstra.MODID);

    public static final Supplier<EnvironmentType> BORDER = ENVIRONMENT_TYPES.register("border", BorderEnvironmentType::new);
    public static final Supplier<EnvironmentType> INHIBITOR = ENVIRONMENT_TYPES.register("inhibitor", InhibitorEnvironmentType::new);

    public static void register(IEventBus eventBus) {
        ENVIRONMENT_TYPES.register(eventBus);
    }
}
