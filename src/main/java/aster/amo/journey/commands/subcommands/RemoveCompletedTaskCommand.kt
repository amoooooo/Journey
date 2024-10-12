package aster.amo.journey.commands.subcommands
import aster.amo.ceremony.utils.extension.get
import aster.amo.journey.data.JourneyDataObject
import com.cobblemon.mod.common.util.removeIf
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import com.mojang.brigadier.tree.LiteralCommandNode
import kotlinx.coroutines.CompletableDeferred
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.server.level.ServerPlayer
import net.minecraft.network.chat.Component
import java.util.concurrent.CompletableFuture

class RemoveCompletedTaskCommand {

    /**
     * Registers the `/journey removecompleted <taskName>` command.
     */
    fun build(): LiteralCommandNode<CommandSourceStack> {
        return Commands.literal("removecompleted")
                        .then(
                            Commands.argument("taskName", StringArgumentType.string())
                                .suggests{ ctx, builder ->
                                    val source = ctx.source
                                    val player = source.playerOrException
                                    val journeyData = player get JourneyDataObject
                                    val completedTasks = journeyData.completedQuests.keys.map { it.toString() }
                                    return@suggests SharedSuggestionProvider.suggest(completedTasks, builder)
                                }
                                .executes(::executeRemoveCompletedTask)
                        ).build()

    }

    /**
     * Executes the command to remove a completed task.
     */
    private fun executeRemoveCompletedTask(context: CommandContext<CommandSourceStack>): Int {
        val source = context.source
        val player = source.player ?: run {
            source.sendFailure(Component.literal("This command can only be used by players."))
            return 0
        }

        val taskName = StringArgumentType.getString(context, "taskName").asResource() // Implement asResource()

        val journeyData = player get JourneyDataObject

        if (!journeyData.completedQuests.keys.map { it.toString() }.contains(taskName)) {
            source.sendFailure(Component.literal("Task '$taskName' is not in your completed quests."))
            return 0
        }

        // Remove the task from completed quests
        journeyData.completedQuests.removeIf { it.toString() == taskName }

        return 1
    }

    /**
     * Extension function to convert a string to a resource identifier.
     * Implement this based on your specific requirements.
     */
    private fun String.asResource(): String {
        // Example implementation; modify as needed
        return this.lowercase().replace(" ", "_")
    }
}
