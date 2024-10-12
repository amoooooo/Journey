package aster.amo.journey.commands.subcommands

import aster.amo.ceremony.utils.extension.get
import aster.amo.journey.Journey
import aster.amo.journey.data.JourneyDataObject
import aster.amo.journey.task.TaskRegistry
import aster.amo.journey.utils.SubCommand
import aster.amo.journey.utils.inform
import aster.amo.journey.utils.parseToNative
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.LiteralCommandNode

import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.commands.arguments.GameProfileArgument
import net.minecraft.commands.arguments.GameProfileArgument.gameProfile
import net.minecraft.commands.arguments.ResourceLocationArgument
import net.minecraft.network.chat.Component
import net.minecraft.commands.arguments.ResourceLocationArgument.id as resourceLocation

class StartTaskCommand : SubCommand {
    override fun build(): LiteralCommandNode<CommandSourceStack> {
        return Commands.literal("starttask")
            .then(
                Commands.argument("player", gameProfile())
                    .then(
                        Commands.argument("task_id", resourceLocation())
                            .suggests { context, builder -> return@suggests SharedSuggestionProvider.suggest(TaskRegistry.TASKS.keys.map { it.toString() }, builder) }
                            .executes(Companion::startTask)
                    )
            )
            .build()
    }

    companion object {
        fun startTask(ctx: CommandContext<CommandSourceStack>): Int {
            val source = ctx.source
            val server = source.server

            val gameProfiles = GameProfileArgument.getGameProfiles(ctx, "player")
            val players = gameProfiles.mapNotNull { server.playerList.getPlayer(it.id) }

            if (players.isEmpty()) {
                source.sendFailure(
                    Component.literal("No valid online players found for the specified name(s).")
                )
                return 0
            }

            // Get the task ID
            val taskId = ResourceLocationArgument.getId(ctx, "task_id")
            val task = TaskRegistry.TASKS[taskId]

            if (task == null) {
                source.sendFailure(
                    Component.literal("Task not found: $taskId")
                )
                return 0
            }

            // Start the task for each player
            players.forEach { player ->
                val data = player get  JourneyDataObject
                if (data.hasTask(taskId)) {
                    player inform "<red>Task already started: ".parseToNative().append(task.name.parseToNative())
                } else {
                    data.addTask(task)
                    player inform "<green>Started task: ".parseToNative().append(task.name.parseToNative())
                }
            }

            return 1
        }
    }
}
