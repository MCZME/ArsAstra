package com.github.mczme.arsastra.client;

import com.github.mczme.arsastra.ArsAstra;
import com.github.mczme.arsastra.client.gui.StarChartJournalScreen;
import com.github.mczme.arsastra.registry.AAMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = ArsAstra.MODID, value = Dist.CLIENT)
public class AAClientEvents {

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(AAMenus.STAR_CHART_JOURNAL_MENU.get(), StarChartJournalScreen::new);
    }
}
