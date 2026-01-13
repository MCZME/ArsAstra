package com.github.mczme.arsastra.command;

import com.github.mczme.arsastra.core.element.profile.ElementProfileManager;
import com.github.mczme.arsastra.core.knowledge.PlayerKnowledge;
import com.github.mczme.arsastra.core.starchart.StarChart;
import com.github.mczme.arsastra.core.starchart.StarChartManager;
import com.github.mczme.arsastra.network.payload.SyncKnowledgePayload;
import com.github.mczme.arsastra.registry.AAAttachments;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Optional;

public class KnowledgeCommands {

    public static int clearKnowledge(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            PlayerKnowledge knowledge = player.getData(AAAttachments.PLAYER_KNOWLEDGE);
            knowledge.clear();
            PacketDistributor.sendToPlayer(player, new SyncKnowledgePayload(knowledge.serializeNBT(player.registryAccess())));
            context.getSource().sendSuccess(() -> Component.literal("Cleared all player knowledge."), true);
            return 1;
        } catch (CommandSyntaxException e) {
            context.getSource().sendFailure(Component.literal(e.getMessage()));
            return 0;
        }
    }

    public static int addKnowledgeItem(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        Item item = ItemArgument.getItem(context, "item").getItem();
        PlayerKnowledge knowledge = player.getData(AAAttachments.PLAYER_KNOWLEDGE);
        
        if (knowledge.analyzeItem(item)) {
            PacketDistributor.sendToPlayer(player, new SyncKnowledgePayload(knowledge.serializeNBT(player.registryAccess())));
            context.getSource().sendSuccess(() -> Component.literal("Added knowledge for item: " + BuiltInRegistries.ITEM.getKey(item)), true);
        } else {
            context.getSource().sendFailure(Component.literal("Player already knows item: " + BuiltInRegistries.ITEM.getKey(item)));
        }
        return 1;
    }

    public static int addKnowledgeChart(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ResourceLocation id = ResourceLocationArgument.getId(context, "chart_id");
        
        if (StarChartManager.getInstance().getStarChart(id).isEmpty()) {
            context.getSource().sendFailure(Component.literal("Star chart does not exist: " + id));
            return 0;
        }

        PlayerKnowledge knowledge = player.getData(AAAttachments.PLAYER_KNOWLEDGE);
        if (knowledge.visitStarChart(id)) {
            PacketDistributor.sendToPlayer(player, new SyncKnowledgePayload(knowledge.serializeNBT(player.registryAccess())));
            context.getSource().sendSuccess(() -> Component.literal("Added knowledge for star chart: " + id), true);
        } else {
            context.getSource().sendFailure(Component.literal("Player already knows star chart: " + id));
        }
        return 1;
    }

    public static int addKnowledgeField(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ResourceLocation id = ResourceLocationArgument.getId(context, "chart_id");
        int index = IntegerArgumentType.getInteger(context, "index");

        Optional<StarChart> chartOpt = StarChartManager.getInstance().getStarChart(id);
        if (chartOpt.isEmpty()) {
            context.getSource().sendFailure(Component.literal("Star chart not found: " + id));
            return 0;
        }
        
        StarChart chart = chartOpt.get();
        if (index < 0 || index >= chart.fields().size()) {
            context.getSource().sendFailure(Component.literal("Invalid field index: " + index + ". Max: " + (chart.fields().size() - 1)));
            return 0;
        }

        PlayerKnowledge knowledge = player.getData(AAAttachments.PLAYER_KNOWLEDGE);
        if (knowledge.unlockField(id, index)) {
            PacketDistributor.sendToPlayer(player, new SyncKnowledgePayload(knowledge.serializeNBT(player.registryAccess())));
            context.getSource().sendSuccess(() -> Component.literal("Unlocked field " + index + " in chart " + id), true);
        } else {
            context.getSource().sendFailure(Component.literal("Player already knows field " + index + " in chart " + id));
        }
        return 1;
    }

    public static int unlockAllKnowledge(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            PlayerKnowledge knowledge = player.getData(AAAttachments.PLAYER_KNOWLEDGE);
            
            int itemCount = 0;
            for (ResourceLocation itemId : ElementProfileManager.getInstance().getAllProfiledItems()) {
                if (knowledge.analyzeItem(BuiltInRegistries.ITEM.get(itemId))) {
                    itemCount++;
                }
            }

            int chartCount = 0;
            int fieldCount = 0;
            for (ResourceLocation chartId : StarChartManager.getInstance().getStarChartIds()) {
                if (knowledge.visitStarChart(chartId)) {
                    chartCount++;
                }
                
                Optional<StarChart> chartOpt = StarChartManager.getInstance().getStarChart(chartId);
                if (chartOpt.isPresent()) {
                    StarChart chart = chartOpt.get();
                    for (int i = 0; i < chart.fields().size(); i++) {
                        if (knowledge.unlockField(chartId, i)) {
                            fieldCount++;
                        }
                    }
                }
            }
            
            PacketDistributor.sendToPlayer(player, new SyncKnowledgePayload(knowledge.serializeNBT(player.registryAccess())));
            
            int finalItemCount = itemCount;
            int finalChartCount = chartCount;
            int finalFieldCount = fieldCount;
            context.getSource().sendSuccess(() -> Component.literal(String.format("Unlocked: %d items, %d charts, %d fields.", finalItemCount, finalChartCount, finalFieldCount)), true);
            return 1;
        } catch (CommandSyntaxException e) {
            context.getSource().sendFailure(Component.literal(e.getMessage()));
            return 0;
        }
    }

    public static int unlockAllStarCharts(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            PlayerKnowledge knowledge = player.getData(AAAttachments.PLAYER_KNOWLEDGE);
            
            int chartCount = 0;
            for (ResourceLocation chartId : StarChartManager.getInstance().getStarChartIds()) {
                if (knowledge.visitStarChart(chartId)) {
                    chartCount++;
                }
            }
            
            PacketDistributor.sendToPlayer(player, new SyncKnowledgePayload(knowledge.serializeNBT(player.registryAccess())));
            
            int finalChartCount = chartCount;
            context.getSource().sendSuccess(() -> Component.literal(String.format("Unlocked %d star charts.", finalChartCount)), true);
            return 1;
        } catch (CommandSyntaxException e) {
            context.getSource().sendFailure(Component.literal(e.getMessage()));
            return 0;
        }
    }

    public static int unlockAllFields(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            PlayerKnowledge knowledge = player.getData(AAAttachments.PLAYER_KNOWLEDGE);
            
            int fieldCount = 0;
            for (ResourceLocation chartId : StarChartManager.getInstance().getStarChartIds()) {
                Optional<StarChart> chartOpt = StarChartManager.getInstance().getStarChart(chartId);
                if (chartOpt.isPresent()) {
                    StarChart chart = chartOpt.get();
                    for (int i = 0; i < chart.fields().size(); i++) {
                        if (knowledge.unlockField(chartId, i)) {
                            fieldCount++;
                        }
                    }
                }
            }
            
            PacketDistributor.sendToPlayer(player, new SyncKnowledgePayload(knowledge.serializeNBT(player.registryAccess())));
            
            int finalFieldCount = fieldCount;
            context.getSource().sendSuccess(() -> Component.literal(String.format("Unlocked %d fields across all charts.", finalFieldCount)), true);
            return 1;
        } catch (CommandSyntaxException e) {
            context.getSource().sendFailure(Component.literal(e.getMessage()));
            return 0;
        }
    }
}