package com.github.mczme.arsastra;

import org.slf4j.Logger;

import com.github.mczme.arsastra.registry.AABlocks;
import com.github.mczme.arsastra.registry.AACreativeTabs;
import com.github.mczme.arsastra.registry.AAItems;
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
    }
}
