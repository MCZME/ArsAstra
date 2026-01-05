package com.github.mczme.arsastra.core;

import com.github.mczme.arsastra.ArsAstra;
import com.github.mczme.arsastra.network.AANetwork;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = ArsAstra.MODID)
public class AAEventHandler {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            // 玩家登录时同步知识数据
            AANetwork.sendToPlayer(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            // 玩家重生时也同步一次，确保客户端数据最新
            AANetwork.sendToPlayer(serverPlayer);
        }
    }
    
    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            // 切换维度时可能需要同步
            AANetwork.sendToPlayer(serverPlayer);
        }
    }
}
