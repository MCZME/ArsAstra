package com.github.mczme.arsastra;

import com.github.mczme.arsastra.registry.*;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;


@Mod(ArsAstra.MODID)
public class ArsAstra {
    public static final String MODID = "ars_astra";
    public static final Logger LOGGER = LogUtils.getLogger();
    
    public ArsAstra(IEventBus modEventBus, ModContainer modContainer) {
        AABlocks.register(modEventBus);
        AAItems.register(modEventBus);
        AACreativeTabs.register(modEventBus);
        AAElements.register(modEventBus);
        AAEnvironmentTypes.register(modEventBus);
        AAComponents.register(modEventBus);
        AAMenus.register(modEventBus);
    }
}
