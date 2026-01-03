package com.github.mczme.arsastra.command;

import com.github.mczme.arsastra.ArsAstra;
import com.github.mczme.arsastra.core.element.profile.ElementProfileManager;
import com.github.mczme.arsastra.core.starchart.StarChart;
import com.github.mczme.arsastra.core.starchart.StarChartManager;
import com.github.mczme.arsastra.core.starchart.engine.AlchemyInput;
import com.github.mczme.arsastra.core.starchart.engine.StarChartRoute;
import com.github.mczme.arsastra.core.starchart.engine.service.RouteGenerationServiceImpl;
import com.github.mczme.arsastra.core.starchart.environment.Environment;
import com.github.mczme.arsastra.core.starchart.path.StarChartPath;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
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
                )
        );
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

        // 尝试从第一个物品获取 launchPoint
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

        // 准备输出内容
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

        // 写入文件
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
}