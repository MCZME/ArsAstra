package com.github.mczme.arsastra.network;

import com.github.mczme.arsastra.ArsAstra;
import com.github.mczme.arsastra.client.gui.StarChartJournalScreen;
import com.github.mczme.arsastra.core.ArsAstraSavedData;
import com.github.mczme.arsastra.core.element.profile.ElementProfileManager;
import com.github.mczme.arsastra.core.knowledge.PlayerKnowledge;
import com.github.mczme.arsastra.core.starchart.StarChart;
import com.github.mczme.arsastra.core.starchart.StarChartManager;
import com.github.mczme.arsastra.core.starchart.engine.DeductionResult;
import com.github.mczme.arsastra.core.starchart.engine.service.DeductionService;
import com.github.mczme.arsastra.core.starchart.engine.service.DeductionServiceImpl;
import com.github.mczme.arsastra.core.manuscript.ManuscriptManager;
import com.github.mczme.arsastra.block.entity.AnalysisDeskBlockEntity;
import com.github.mczme.arsastra.network.payload.*;
import com.github.mczme.arsastra.registry.AAAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
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
                SyncEnvironmentPayload.TYPE,
                SyncEnvironmentPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        ManuscriptManager.getInstance().setCurrentSeedHash(payload.seedHash());
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

        registrar.playToClient(
                AnalysisResultPayload.TYPE,
                AnalysisResultPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        if (Minecraft.getInstance().screen instanceof com.github.mczme.arsastra.client.gui.AnalysisDeskScreen screen) {
                            screen.showStatusMessage(payload.message(), payload.isError());
                        } else {
                            context.player().displayClientMessage(payload.message(), true);
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

        registrar.playToServer(
                AnalysisActionPayload.TYPE,
                AnalysisActionPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        ServerPlayer player = (ServerPlayer) context.player();
                        Level level = player.level();
                        BlockPos pos = payload.pos();
                        
                        // 简单的距离校验，防止远程交互
                        if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64) {
                            return;
                        }

                        BlockEntity be = level.getBlockEntity(pos);
                        if (be instanceof AnalysisDeskBlockEntity analysisDesk) {
                            switch (payload.action()) {
                                case DIRECT_ANALYSIS -> analysisDesk.serverPerformDirectAnalysis(player);
                                case START_GUESS -> analysisDesk.serverStartIntuitionAnalysis(player);
                                case SUBMIT_GUESS -> analysisDesk.serverSubmitGuess(player, payload.guesses());
                                case BATCH_ANALYSIS -> analysisDesk.serverPerformBatchAnalysis(player);
                            }
                        }
                    });
                }
        );
    }

    public static void sendToPlayer(ServerPlayer player) {
        PlayerKnowledge knowledge = player.getData(AAAttachments.PLAYER_KNOWLEDGE);
        PacketDistributor.sendToPlayer(player, new SyncKnowledgePayload(knowledge.serializeNBT(player.registryAccess())));
        sendEnvironmentToPlayer(player);
    }

    public static void sendEnvironmentToPlayer(ServerPlayer player) {
        long seed = ArsAstraSavedData.get(player.serverLevel()).getElementProfileSeed();
        String seedHash = Long.toHexString(seed);
        PacketDistributor.sendToPlayer(player, new SyncEnvironmentPayload(seedHash));
    }
}
