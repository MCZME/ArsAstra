package com.github.mczme.arsastra.core;

import com.github.mczme.arsastra.ArsAstra;
import com.github.mczme.arsastra.core.element.profile.ElementProfileManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

@EventBusSubscriber(modid = ArsAstra.MODID)
public class AAEvent {
    private static boolean tagsReady = false;
    private static boolean seedRetrieved = false;
    private static long customSeed = 0L;
    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(ElementProfileManager.getInstance());
    }

    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        if (event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) {
            tagsReady = true;
            tryProcessDefinitions();
        }
    }

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        event.getServer();
    }

    @SubscribeEvent
    public static void onWorldLoad(LevelEvent.Load event) {
        // 只在逻辑服务器端的主世界执行一次
        if (event.getLevel() instanceof ServerLevel serverLevel && serverLevel.dimension() == Level.OVERWORLD) {
            if (!seedRetrieved) {
                ArsAstraSavedData savedData = ArsAstraSavedData.get(serverLevel);
                customSeed = savedData.getElementProfileSeed();
                seedRetrieved = true;
                tryProcessDefinitions();
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        tagsReady = false;
        seedRetrieved = false;
        customSeed = 0L;
    }

    private static void tryProcessDefinitions() {
        // 只有当标签准备就绪且世界种子已获取时才进行处理
        if (tagsReady && seedRetrieved) {
            ArsAstra.LOGGER.debug("All conditions met. Processing element definitions with custom seed {}.", customSeed);
            ElementProfileManager.getInstance().processDefinitions(customSeed);
            // 重置tagsReady，为下一次/reload做准备
            tagsReady = false;
        }
    }
}
