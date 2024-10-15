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

class CompleteSubtaskCommand : SubCommand {
    override fun build(): LiteralCommandNode<CommandSourceStack> {
        return Commands.literal("completesubtask")
            .then(
                Commands.argument("player", GameProfileArgument.gameProfile())
                    .then(
                        Commands.argument("task_id", ResourceLocationArgument.id())
                            .suggests { context, builder ->
                                SharedSuggestionProvider.suggest(TaskRegistry.TASKS.keys.map { it.toString() }, builder)
                            }
                            .then(
                                Commands.argument("subtask_id", ResourceLocationArgument.id())
                                    .suggests { context, builder ->
                                        val taskId = ResourceLocationArgument.getId(context, "task_id")
                                        val task = TaskRegistry.TASKS[taskId]
                                        if (task != null) {
                                            SharedSuggestionProvider.suggest(task.tasks.map { it.id }, builder)
                                        } else {
                                            builder.buildFuture()
                                        }
                                    }
                                    .executes(Companion::completeSubtask)
                            )
                    )
            )
            .build()
    }

    companion object {
        fun completeSubtask(ctx: CommandContext<CommandSourceStack>): Int {
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

            // Get the task and subtask IDs
            val taskId: ResourceLocation = ResourceLocationArgument.getId(ctx, "task_id")
            val subtaskId = ResourceLocationArgument.getId(ctx, "subtask_id").toString()
            val task = TaskRegistry.TASKS[taskId]

            if (task == null) {
                source.sendFailure(
                    Component.literal("Task not found: $taskId")
                )
                return 0
            }

            val subtask = task.tasks.firstOrNull { it.id == subtaskId }

            if (subtask == null) {
                source.sendFailure(
                    Component.literal("Subtask not found: $subtaskId in task $taskId")
                )
                return 0
            }

            // Complete the subtask for each player
            players.forEach { player ->
                val data = player get JourneyDataObject
                val taskProgress = data.tasksProgress[taskId]
                if (taskProgress != null) {
                    val subtaskProgress = taskProgress.subtasksProgress[subtaskId]
                    val registeredSubtask = TaskRegistry.getSubtask(taskId, subtaskId) ?: return@forEach
                    if (subtaskProgress != null) {
                        subtaskProgress.progress = registeredSubtask.target
                        registeredSubtask.rewards.forEach { it.parse(player as ServerPlayer) }
                        data.checkAndCompleteTask(taskId, subtask, subtaskProgress)
                        player inform "<green>Subtask completed: ".parseToNative().append(subtask.name.parseToNative())
                        data.rebuildSidebar()
                    } else {
                        player inform "<red>Subtask not active or already completed: ".parseToNative().append(subtask.name.parseToNative())
                    }
                } else {
                    player inform "<red>Task not active: ".parseToNative().append(task.name.parseToNative())
                }
            }

            return 1
        }
    }
}
