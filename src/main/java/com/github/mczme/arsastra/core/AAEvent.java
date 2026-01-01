package com.github.mczme.arsastra.core;

import com.github.mczme.arsastra.ArsAstra;
import com.github.mczme.arsastra.core.element.profile.ElementProfileManager;
import com.github.mczme.arsastra.core.starchart.StarChartManager;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = ArsAstra.MODID)
public class AAEvent {

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(ElementProfileManager.getInstance());
        event.addListener(StarChartManager.getInstance());
    }
}
