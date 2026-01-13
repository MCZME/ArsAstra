package com.github.mczme.arsastra.command;

import com.github.mczme.arsastra.ArsAstra;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = ArsAstra.MODID)
public class AACommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("arsastra")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("knowledge")
                        .then(Commands.literal("clear")
                                .executes(KnowledgeCommands::clearKnowledge)
                        )
                        .then(Commands.literal("add")
                                .then(Commands.literal("item")
                                        .then(Commands.argument("item", ItemArgument.item(event.getBuildContext()))
                                                .executes(KnowledgeCommands::addKnowledgeItem)
                                        )
                                )
                                .then(Commands.literal("chart")
                                        .then(Commands.argument("chart_id", ResourceLocationArgument.id())
                                                .executes(KnowledgeCommands::addKnowledgeChart)
                                        )
                                )
                                .then(Commands.literal("effect")
                                        .then(Commands.argument("effect_id", ResourceLocationArgument.id())
                                                .executes(KnowledgeCommands::addKnowledgeEffect)
                                        )
                                )
                        )
                        .then(Commands.literal("unlock_all")
                                .executes(KnowledgeCommands::unlockAllKnowledge)
                        )
                        .then(Commands.literal("unlock_starcharts")
                                .executes(KnowledgeCommands::unlockAllStarCharts)
                        )
                        .then(Commands.literal("unlock_effects")
                                .executes(KnowledgeCommands::unlockAllEffects)
                        )
                )
                .then(Commands.literal("debug")
                        .then(Commands.literal("test_env")
                                .then(Commands.argument("chart", ResourceLocationArgument.id())
                                        .then(Commands.argument("x", FloatArgumentType.floatArg())
                                                .then(Commands.argument("y", FloatArgumentType.floatArg())
                                                        .executes(DebugCommands::testEnvironment)
                                                )
                                        )
                                )
                        )
                        .then(Commands.literal("route")
                                .then(Commands.argument("chart", ResourceLocationArgument.id())
                                        .then(Commands.argument("item", ItemArgument.item(event.getBuildContext()))
                                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                                        .executes(DebugCommands::testRoute)
                                                )
                                        )
                                )
                        )
                        .then(Commands.literal("deduce")
                                .then(Commands.argument("chart", ResourceLocationArgument.id())
                                        .then(Commands.argument("item", ItemArgument.item(event.getBuildContext()))
                                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                                        .then(Commands.argument("rotation", FloatArgumentType.floatArg())
                                                                .executes(DebugCommands::testDeduction)
                                                        )
                                                        .executes(DebugCommands::testDeduction)
                                                )
                                        )
                                )
                        )
                )
        );
    }
}
