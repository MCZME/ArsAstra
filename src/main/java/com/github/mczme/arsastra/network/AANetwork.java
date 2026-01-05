package com.github.mczme.arsastra.network;

import com.github.mczme.arsastra.ArsAstra;
import com.github.mczme.arsastra.client.gui.StarChartJournalScreen;
import com.github.mczme.arsastra.core.knowledge.PlayerKnowledge;
import com.github.mczme.arsastra.core.starchart.StarChart;
import com.github.mczme.arsastra.core.starchart.StarChartManager;
import com.github.mczme.arsastra.core.starchart.engine.StarChartContext;
import com.github.mczme.arsastra.core.starchart.engine.StarChartEngine;
import com.github.mczme.arsastra.core.starchart.engine.StarChartEngineImpl;
import com.github.mczme.arsastra.core.starchart.engine.StarChartRoute;
import com.github.mczme.arsastra.core.starchart.path.StarChartPath;
import com.github.mczme.arsastra.network.payload.DeductionResultPayload;
import com.github.mczme.arsastra.network.payload.RequestDeductionPayload;
import com.github.mczme.arsastra.network.payload.SyncKnowledgePayload;
import com.github.mczme.arsastra.registry.AAAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = ArsAstra.MODID)
public class AANetwork {

    private static final StarChartEngine ENGINE = new StarChartEngineImpl();

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        
        registrar.playToClient(
                SyncKnowledgePayload.TYPE,
                SyncKnowledgePayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        var player = context.player();
                        if (player != null) {
                            PlayerKnowledge knowledge = player.getData(AAAttachments.PLAYER_KNOWLEDGE);
                            knowledge.deserializeNBT(player.registryAccess(), payload.data());
                        }
                    });
                }
        );

        registrar.playToClient(
                DeductionResultPayload.TYPE,
                DeductionResultPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        if (Minecraft.getInstance().screen instanceof StarChartJournalScreen screen) {
                            screen.handleDeductionResult(payload);
                        }
                    });
                }
        );

        registrar.playToServer(
                RequestDeductionPayload.TYPE,
                RequestDeductionPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        ServerPlayer player = (ServerPlayer) context.player();
                        StarChart chart = StarChartManager.getInstance()
                                .getStarChart(ResourceLocation.fromNamespaceAndPath("ars_astra", "base_chart"))
                                .orElse(null);
                        
                        if (chart != null) {
                            StarChartContext computeContext = new StarChartContext(
                                    payload.items(), StarChartRoute.EMPTY, List.of(), 1.0f, Map.of());
                            StarChartContext result = ENGINE.compute(chart, computeContext, new Vector2f(0, 0));
                            
                            // 采样航线点以供渲染
                            List<Vector2f> points = new ArrayList<>();
                            for (StarChartPath segment : result.currentRoute().segments()) {
                                float len = segment.getLength();
                                for (float d = 0; d < len; d += 2.0f) {
                                    points.add(segment.getPointAtDistance(d));
                                }
                                points.add(segment.getEndPoint());
                            }
                            
                            PacketDistributor.sendToPlayer(player, new DeductionResultPayload(points, result.stability()));
                        }
                    });
                }
        );
    }

    public static void sendToPlayer(ServerPlayer player) {
        PlayerKnowledge knowledge = player.getData(AAAttachments.PLAYER_KNOWLEDGE);
        PacketDistributor.sendToPlayer(player, new SyncKnowledgePayload(knowledge.serializeNBT(player.registryAccess())));
    }
}
