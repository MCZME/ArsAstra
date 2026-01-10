package com.github.mczme.arsastra.network;

import com.github.mczme.arsastra.ArsAstra;
import com.github.mczme.arsastra.client.gui.StarChartJournalScreen;
import com.github.mczme.arsastra.core.element.profile.ElementProfileManager;
import com.github.mczme.arsastra.core.knowledge.PlayerKnowledge;
import com.github.mczme.arsastra.core.starchart.StarChart;
import com.github.mczme.arsastra.core.starchart.StarChartManager;
import com.github.mczme.arsastra.core.starchart.engine.DeductionResult;
import com.github.mczme.arsastra.core.starchart.engine.service.DeductionService;
import com.github.mczme.arsastra.core.starchart.engine.service.DeductionServiceImpl;
import com.github.mczme.arsastra.network.payload.DeductionResultPayload;
import com.github.mczme.arsastra.network.payload.RequestDeductionPayload;
import com.github.mczme.arsastra.network.payload.SyncKnowledgePayload;
import com.github.mczme.arsastra.registry.AAAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.joml.Vector2f;

@EventBusSubscriber(modid = ArsAstra.MODID)
public class AANetwork {

    private static final DeductionService DEDUCTION_SERVICE = new DeductionServiceImpl();

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
                        if (payload.inputs().isEmpty()) return;

                        StarChart chart = StarChartManager.getInstance()
                                .getStarChart(payload.starChartId())
                                .orElse(null);
                        
                        if (chart != null) {
                            ItemStack firstStack = payload.inputs().get(0).stack();
                            Vector2f startPoint = ElementProfileManager.getInstance()
                                    .getElementProfile(firstStack.getItem())
                                    .map(p -> p.launchPoint())
                                    .orElse(new Vector2f(0, 0));

                            DeductionResult result = DEDUCTION_SERVICE.deduce(chart, payload.inputs(), startPoint);
                            
                            PacketDistributor.sendToPlayer(player, new DeductionResultPayload(result));
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
