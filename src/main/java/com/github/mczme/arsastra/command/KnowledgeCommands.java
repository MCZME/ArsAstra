package com.github.mczme.arsastra.command;

import com.github.mczme.arsastra.core.element.profile.ElementProfileManager;
import com.github.mczme.arsastra.core.knowledge.PlayerKnowledge;
import com.github.mczme.arsastra.core.starchart.StarChartManager;
import com.github.mczme.arsastra.network.payload.SyncKnowledgePayload;
import com.github.mczme.arsastra.registry.AAAttachments;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.network.PacketDistributor;

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

    public static int addKnowledgeEffect(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ResourceLocation id = ResourceLocationArgument.getId(context, "effect_id");
        MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(id);
        
        if (effect == null) {
            context.getSource().sendFailure(Component.literal("Mob effect not found: " + id));
            return 0;
        }

        PlayerKnowledge knowledge = player.getData(AAAttachments.PLAYER_KNOWLEDGE);
        if (knowledge.learnEffect(effect)) {
            PacketDistributor.sendToPlayer(player, new SyncKnowledgePayload(knowledge.serializeNBT(player.registryAccess())));
            context.getSource().sendSuccess(() -> Component.literal("Added knowledge for effect: " + id), true);
        } else {
            context.getSource().sendFailure(Component.literal("Player already knows effect: " + id));
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
            for (ResourceLocation chartId : StarChartManager.getInstance().getStarChartIds()) {
                if (knowledge.visitStarChart(chartId)) {
                    chartCount++;
                }
            }

            int effectCount = 0;
            for (MobEffect effect : BuiltInRegistries.MOB_EFFECT) {
                if (knowledge.learnEffect(effect)) {
                    effectCount++;
                }
            }
            
            // 同步到客户端
            PacketDistributor.sendToPlayer(player, new SyncKnowledgePayload(knowledge.serializeNBT(player.registryAccess())));
            
            int finalItemCount = itemCount;
            int finalChartCount = chartCount;
            int finalEffectCount = effectCount;
            context.getSource().sendSuccess(() -> Component.literal(String.format("Unlocked: %d items, %d charts, %d effects.", finalItemCount, finalChartCount, finalEffectCount)), true);
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
            
            // 同步到客户端
            PacketDistributor.sendToPlayer(player, new SyncKnowledgePayload(knowledge.serializeNBT(player.registryAccess())));
            
            int finalChartCount = chartCount;
            context.getSource().sendSuccess(() -> Component.literal(String.format("Unlocked %d star charts.", finalChartCount)), true);
            return 1;
        } catch (CommandSyntaxException e) {
            context.getSource().sendFailure(Component.literal(e.getMessage()));
            return 0;
        }
    }

    public static int unlockAllEffects(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            PlayerKnowledge knowledge = player.getData(AAAttachments.PLAYER_KNOWLEDGE);
            
            int effectCount = 0;
            for (MobEffect effect : BuiltInRegistries.MOB_EFFECT) {
                if (knowledge.learnEffect(effect)) {
                    effectCount++;
                }
            }
            
            // 同步到客户端
            PacketDistributor.sendToPlayer(player, new SyncKnowledgePayload(knowledge.serializeNBT(player.registryAccess())));
            
            int finalEffectCount = effectCount;
            context.getSource().sendSuccess(() -> Component.literal(String.format("Unlocked %d effects.", finalEffectCount)), true);
            return 1;
        } catch (CommandSyntaxException e) {
            context.getSource().sendFailure(Component.literal(e.getMessage()));
            return 0;
        }
    }
}