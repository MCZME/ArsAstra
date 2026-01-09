package com.github.mczme.arsastra.command;

import com.github.mczme.arsastra.ArsAstra;
import com.github.mczme.arsastra.core.element.profile.ElementProfileManager;
import com.github.mczme.arsastra.core.knowledge.PlayerKnowledge;
import com.github.mczme.arsastra.core.starchart.StarChart;
import com.github.mczme.arsastra.core.starchart.StarChartManager;
import com.github.mczme.arsastra.core.starchart.engine.AlchemyInput;
import com.github.mczme.arsastra.core.starchart.engine.DeductionResult;
import com.github.mczme.arsastra.core.starchart.engine.StarChartRoute;
import com.github.mczme.arsastra.core.starchart.engine.service.*;
import com.github.mczme.arsastra.core.starchart.environment.Environment;
import com.github.mczme.arsastra.core.starchart.path.StarChartPath;
import com.github.mczme.arsastra.network.payload.SyncKnowledgePayload;
import com.github.mczme.arsastra.registry.AAAttachments;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector2f;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@EventBusSubscriber(modid = ArsAstra.MODID)
public class AACommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("arsastra")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("knowledge")
                        .then(Commands.literal("unlock_all")
                                .executes(AACommands::unlockAllKnowledge)
                        )
                        .then(Commands.literal("unlock_starcharts")
                                .executes(AACommands::unlockAllStarCharts)
                        )
                )
                .then(Commands.literal("debug")
                        .then(Commands.literal("test_env")
                                .then(Commands.argument("chart", ResourceLocationArgument.id())
                                        .then(Commands.argument("x", FloatArgumentType.floatArg())
                                                .then(Commands.argument("y", FloatArgumentType.floatArg())
                                                        .executes(AACommands::testEnvironment)
                                                )
                                        )
                                )
                        )
                        .then(Commands.literal("route")
                                .then(Commands.argument("chart", ResourceLocationArgument.id())
                                        .then(Commands.argument("item", ItemArgument.item(event.getBuildContext()))
                                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                                        .executes(AACommands::testRoute)
                                                )
                                        )
                                )
                        )
                        .then(Commands.literal("deduce")
                                .then(Commands.argument("chart", ResourceLocationArgument.id())
                                        .then(Commands.argument("item", ItemArgument.item(event.getBuildContext()))
                                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                                        .then(Commands.argument("rotation", FloatArgumentType.floatArg())
                                                                .executes(AACommands::testDeduction)
                                                        )
                                                        .executes(AACommands::testDeduction)
                                                )
                                        )
                                )
                        )
                )
        );
    }

    private static int unlockAllKnowledge(CommandContext<CommandSourceStack> context) {
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
            
            // 同步到客户端
            PacketDistributor.sendToPlayer(player, new SyncKnowledgePayload(knowledge.serializeNBT(player.registryAccess())));
            
            int finalItemCount = itemCount;
            int finalChartCount = chartCount;
            context.getSource().sendSuccess(() -> Component.literal(String.format("Unlocked knowledge: %d items, %d star charts.", finalItemCount, finalChartCount)), true);
            return 1;
        } catch (CommandSyntaxException e) {
            context.getSource().sendFailure(Component.literal(e.getMessage()));
            return 0;
        }
    }

    private static int unlockAllStarCharts(CommandContext<CommandSourceStack> context) {
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

    private static int testEnvironment(CommandContext<CommandSourceStack> context) {
        ResourceLocation chartId = ResourceLocationArgument.getId(context, "chart");
        float x = FloatArgumentType.getFloat(context, "x");
        float y = FloatArgumentType.getFloat(context, "y");
        Vector2f pos = new Vector2f(x, y);

        Optional<StarChart> chartOpt = StarChartManager.getInstance().getStarChart(chartId);
        if (chartOpt.isEmpty()) {
            context.getSource().sendFailure(Component.literal("StarChart not found: " + chartId));
            return 0;
        }
        StarChart chart = chartOpt.get();

        Environment hitEnv = null;
        for (Environment env : chart.environments()) {
            if (env.shape().contains(pos)) {
                hitEnv = env;
                break;
            }
        }

        if (hitEnv != null) {
            String typeDesc = hitEnv.getType().getDescriptionId();
            context.getSource().sendSuccess(() -> Component.literal(
                    String.format("Hit Environment at (%.1f, %.1f): Type=%s", x, y, typeDesc)), true);
        } else {
            context.getSource().sendSuccess(() -> Component.literal(
                    String.format("No Environment at (%.1f, %.1f)", x, y)), true);
        }
        return 1;
    }

    private static int testRoute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ResourceLocation chartId = ResourceLocationArgument.getId(context, "chart");
        int count = IntegerArgumentType.getInteger(context, "count");
        
        java.util.List<ItemStack> itemList = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            itemList.add(ItemArgument.getItem(context, "item").createItemStack(1, false));
        }
        
        Optional<StarChart> chartOpt = StarChartManager.getInstance().getStarChart(chartId);
        if (chartOpt.isEmpty()) {
            context.getSource().sendFailure(Component.literal("StarChart not found: " + chartId));
            return 0;
        }

        Vector2f startPos = new Vector2f(0, 0);
        if (!itemList.isEmpty()) {
            var profileOpt = ElementProfileManager.getInstance().getElementProfile(itemList.get(0).getItem());
            if (profileOpt.isPresent()) {
                startPos.set(profileOpt.get().launchPoint());
            }
        }

        RouteGenerationServiceImpl routeService = new RouteGenerationServiceImpl();
        List<AlchemyInput> inputs = itemList.stream().map(AlchemyInput::of).collect(Collectors.toList());
        StarChartRoute route = routeService.computeRoute(inputs, startPos, chartOpt.get());

        Vector2f endPoint = new Vector2f(0,0);
        if (!route.segments().isEmpty()) {
            endPoint = route.segments().get(route.segments().size() - 1).getEndPoint();
        }

        StringBuilder sb = new StringBuilder();
        String itemName = itemList.isEmpty() ? "None" : itemList.get(0).getItem().toString();
        sb.append(String.format("Route Generated for %s x%d:\n", itemName, count));
        sb.append(String.format(" - Segments: %d\n", route.segments().size()));
        sb.append(String.format(" - Total Length: %.3f\n", route.getTotalLength()));
        sb.append(String.format(" - Start Point: (%.3f, %.3f)\n", startPos.x, startPos.y));
        sb.append(String.format(" - End Point: (%.3f, %.3f)\n", endPoint.x, endPoint.y));
        sb.append("\n--- Detailed Segments ---\n");
        
        int i = 0;
        for (StarChartPath seg : route.segments()) {
             sb.append(String.format("Segment %d [%s]:\n", i++, seg.getClass().getSimpleName()));
             sb.append(String.format("  Start:  (%.3f, %.3f)\n", seg.getStartPoint().x, seg.getStartPoint().y));
             sb.append(String.format("  End:    (%.3f, %.3f)\n", seg.getEndPoint().x, seg.getEndPoint().y));
             sb.append(String.format("  Length: %.3f\n", seg.getLength()));
             sb.append("\n");
        }

        try {
            File debugDir = new File("ars_astra_debug");
            if (!debugDir.exists()) {
                debugDir.mkdirs();
            }
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
            File file = new File(debugDir, "route_" + timestamp + ".txt");
            
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(sb.toString());
            }
            
            String msg = String.format("Route dump saved to: %s (Len: %.1f)", file.getName(), route.getTotalLength());
            context.getSource().sendSuccess(() -> Component.literal(msg), true);
            
        } catch (IOException e) {
            context.getSource().sendFailure(Component.literal("Failed to save route dump: " + e.getMessage()));
            e.printStackTrace();
        }

            return 1;
    }

    private static int testDeduction(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ResourceLocation chartId = ResourceLocationArgument.getId(context, "chart");
        int count = IntegerArgumentType.getInteger(context, "count");
        float rotation = 0;
        try {
            rotation = FloatArgumentType.getFloat(context, "rotation");
        } catch (IllegalArgumentException ignored) {}

        ItemStack stack = ItemArgument.getItem(context, "item").createItemStack(1, false);
        java.util.List<AlchemyInput> inputs = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            inputs.add(new AlchemyInput(stack, rotation));
        }

        Optional<StarChart> chartOpt = StarChartManager.getInstance().getStarChart(chartId);
        if (chartOpt.isEmpty()) {
            context.getSource().sendFailure(Component.literal("StarChart not found: " + chartId));
            return 0;
        }

        Vector2f startPos = new Vector2f(0, 0);
        var profileOpt = ElementProfileManager.getInstance().getElementProfile(stack.getItem());
        profileOpt.ifPresent(p -> startPos.set(p.launchPoint()));

        DeductionService deductionService = new DeductionServiceImpl();
        DeductionResult result = deductionService.deduce(chartOpt.get(), inputs, startPos);

        // 准备输出内容
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Deduction Report for %s x%d (Rot: %.2f rad):\n", stack.getItem(), count, rotation));
        sb.append(String.format(" - Final Stability: %.3f\n", result.finalStability()));
        sb.append(String.format(" - Total Path Segments: %d\n", result.route().segments().size()));
        sb.append(String.format(" - Total Path Length: %.3f\n", result.route().getTotalLength()));

        sb.append("\n--- Predicted Effects ---\n");
        if (result.predictedEffects().isEmpty()) {
            sb.append(" None\n");
        } else {
            result.predictedEffects().forEach((field, data) -> {
                sb.append(String.format(" - %s: Level %d, Duration %d ticks\n", 
                        field.effect(), data.level() + 1, data.duration()));
            });
        }

        sb.append("\n--- Path Segment Details ---\n");
        int i = 0;
        for (StarChartPath path : result.route().segments()) {
            sb.append(String.format("Segment %d [%s]:\n", i++, path.getClass().getSimpleName()));
            sb.append(String.format("  Start:  (%.3f, %.3f)\n", path.getStartPoint().x, path.getStartPoint().y));
            sb.append(String.format("  End:    (%.3f, %.3f)\n", path.getEndPoint().x, path.getEndPoint().y));
            sb.append(String.format("  Length: %.3f\n", path.getLength()));
            sb.append("\n");
        }

        // 写入文件
        try {
            File debugDir = new File("ars_astra_debug");
            if (!debugDir.exists()) debugDir.mkdirs();
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
            File file = new File(debugDir, "deduction_" + timestamp + ".txt");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(sb.toString());
            }
            context.getSource().sendSuccess(() -> Component.literal("Deduction report saved: " + file.getName()), true);
        } catch (IOException e) {
            context.getSource().sendFailure(Component.literal("Failed to save report: " + e.getMessage()));
        }

        return 1;
    }

}