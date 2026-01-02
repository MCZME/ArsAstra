package com.github.mczme.arsastra.registry;

import com.github.mczme.arsastra.ArsAstra;
import com.github.mczme.arsastra.core.element.Element;
import com.github.mczme.arsastra.core.starchart.environment.EnvironmentType;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@EventBusSubscriber(modid = ArsAstra.MODID)
public class AARegistries {
    public static final Logger LOGGER = LoggerFactory.getLogger(AARegistries.class);

    // Element Registry
    public static final ResourceKey<Registry<Element>> ELEMENT_REGISTRY_KEY = ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(ArsAstra.MODID, "elements"));
    public static final Registry<Element> ELEMENT_REGISTRY = new RegistryBuilder<>(ELEMENT_REGISTRY_KEY)
            .sync(true)
            .create();

    // Environment Type Registry
    public static final ResourceKey<Registry<EnvironmentType>> ENVIRONMENT_TYPE_REGISTRY_KEY = ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(ArsAstra.MODID, "environment_types"));
    public static final Registry<EnvironmentType> ENVIRONMENT_TYPE_REGISTRY = new RegistryBuilder<>(ENVIRONMENT_TYPE_REGISTRY_KEY)
            .sync(true)
            .create();




    @SubscribeEvent
    public static void registerRegistries(NewRegistryEvent event) {
        LOGGER.info("Registering custom registries for Ars Astra");
        event.register(ELEMENT_REGISTRY);
        event.register(ENVIRONMENT_TYPE_REGISTRY);
    }


}