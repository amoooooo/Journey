package aster.amo.journey.commands.subcommands

import aster.amo.ceremony.utils.extension.get
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
import net.minecraft.commands.arguments.ResourceLocationArgument
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer

class CompleteTaskCommand : SubCommand {
    override fun build(): LiteralCommandNode<CommandSourceStack> {
        return Commands.literal("completetask")
            .then(
                Commands.argument("player", GameProfileArgument.gameProfile())
                    .then(
                        Commands.argument("task_id", ResourceLocationArgument.id())
                            .suggests { context, builder ->
                                val player = context.source.playerOrException as ServerPlayer
                                val data = player get JourneyDataObject
                                SharedSuggestionProvider.suggest(data.tasksProgress.keys.map { it.toString() }, builder)
                            }
                            .executes(Companion::completeTask)
                    )
            )
            .build()
    }

    companion object {
        fun completeTask(ctx: CommandContext<CommandSourceStack>): Int {
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
            val taskId: ResourceLocation = ResourceLocationArgument.getId(ctx, "task_id")
            val task = TaskRegistry.TASKS[taskId]

            if (task == null) {
                source.sendFailure(
                    Component.literal("Task not found: $taskId")
                )
                return 0
            }

            // Complete the task for each player
            players.forEach { player ->
                val data = player get JourneyDataObject
                val taskProgress = data.tasksProgress[taskId]
                if (taskProgress != null) {
                    // Mark all subtasks as completed
                    taskProgress.subtasksProgress.forEach Marker@ { (subtaskId, subtaskProgress) ->
                        val subtask = taskProgress.subtasksProgress[subtaskId]
                        val registeredSubtask = TaskRegistry.getSubtask(taskId, subtaskId) ?: return@Marker
                        if (subtask != null && subtaskProgress.progress != registeredSubtask.target) {
                            subtaskProgress.progress = registeredSubtask.target
                            registeredSubtask.rewards.forEach { it.parse(player as ServerPlayer) }
                            data.checkAndCompleteTask(taskId, registeredSubtask, subtaskProgress)
                        }
                    }
                    player inform "<green>Task completed: ".parseToNative().append(task.name.parseToNative())
                    data.rebuildSidebar()
                } else {
                    player inform "<red>Task not active: ".parseToNative().append(task.name.parseToNative())
                }
            }

            return 1
        }
    }
}
