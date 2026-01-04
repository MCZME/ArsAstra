package com.github.mczme.arsastra.network;

import com.github.mczme.arsastra.ArsAstra;
import com.github.mczme.arsastra.core.knowledge.PlayerKnowledge;
import com.github.mczme.arsastra.network.payload.SyncKnowledgePayload;
import com.github.mczme.arsastra.registry.AAAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = ArsAstra.MODID, bus = EventBusSubscriber.Bus.MOD)
public class AANetwork {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(
                SyncKnowledgePayload.TYPE,
                SyncKnowledgePayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        // 客户端接收并反序列化数据
                        var player = context.player();
                        if (player != null) {
                            PlayerKnowledge knowledge = player.getData(AAAttachments.PLAYER_KNOWLEDGE);
                            // 注意：客户端可能没有完整的 RegistryAccess，但 NBT 反序列化通常只需要 ID
                            knowledge.deserializeNBT(player.registryAccess(), payload.data());
                        }
                    });
                }
        );
    }

    /**
     * 发送玩家的知识数据给玩家自己 (用于同步)
     */
    public static void sendToPlayer(ServerPlayer player) {
        PlayerKnowledge knowledge = player.getData(AAAttachments.PLAYER_KNOWLEDGE);
        // 发送序列化后的 NBT 数据
        PacketDistributor.sendToPlayer(player, new SyncKnowledgePayload(knowledge.serializeNBT(player.registryAccess())));
    }
}
