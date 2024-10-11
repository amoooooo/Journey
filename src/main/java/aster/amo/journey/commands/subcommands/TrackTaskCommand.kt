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
import me.lucko.fabric.api.permissions.v0.Permissions
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.commands.arguments.ResourceLocationArgument
import net.minecraft.network.chat.Component
import net.minecraft.commands.arguments.ResourceLocationArgument.id as resourceLocation

class TrackTaskCommand : SubCommand {
    override fun build(): LiteralCommandNode<CommandSourceStack> {
        return Commands.literal("tracktask")
            .requires(Permissions.require("${Journey.MOD_ID}.command.tracktask", 0)) // Permission level 0 for all players
            .then(
                Commands.argument("task_id", resourceLocation())
                    .suggests { context, builder ->
                        val source = context.source
                        val player = source.playerOrException
                        val data = player.get(JourneyDataObject)
                        val taskIds = data.tasksProgress.keys
                        SharedSuggestionProvider.suggest(taskIds.map { it.toString() }, builder)
                    }
                    .executes(Companion::trackTask)
            )
            .then(
                Commands.literal("untrack")
                    .executes(Companion::untrackTask)
            )
            .build()
    }

    companion object {
        fun trackTask(ctx: CommandContext<CommandSourceStack>): Int {
            val source = ctx.source
            val player = source.playerOrException

            // Get the task ID
            val taskId = ResourceLocationArgument.getId(ctx, "task_id")
            val data = player.get(JourneyDataObject)
            val taskProgress = data.tasksProgress[taskId]

            if (taskProgress != null) {
                data.setTrackedTask(taskId)
                player inform "<green>Now tracking task: ".parseToNative().append(taskId.toString().parseToNative())
            } else {
                player inform "<red>Task not found or not active: ".parseToNative().append(taskId.toString().parseToNative())
            }
            data.rebuildSidebar()
            return 1
        }

        fun untrackTask(ctx: CommandContext<CommandSourceStack>): Int {
            val source = ctx.source
            val player = source.playerOrException

            val data = player.get(JourneyDataObject)
            data.setTrackedTask(null)
            player inform "<yellow>Stopped tracking any task.".parseToNative()

            return 1
        }
    }
}
