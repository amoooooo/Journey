package aster.amo.journey.commands.subcommands
import aster.amo.ceremony.utils.extension.get
import aster.amo.journey.data.JourneyDataObject
import aster.amo.journey.utils.inform
import aster.amo.journey.utils.parseToNative
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
import net.minecraft.commands.arguments.ResourceLocationArgument
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
                            Commands.argument("task", ResourceLocationArgument.id())
                                .suggests{ ctx, builder ->
                                    val source = ctx.source
                                    val player = source.playerOrException
                                    val journeyData = player get JourneyDataObject
                                    val completedTasks = journeyData.completedQuests.keys.map { it.toString() }
                                    return@suggests SharedSuggestionProvider.suggest(completedTasks, builder)
                                }
                                .executes { context ->
                                    val source = context.source
                                    val player = source.player ?: run {
                                        source.sendFailure(Component.literal("This command can only be used by players."))
                                        return@executes 0
                                    }

                                    val taskName = ResourceLocationArgument.getId(context, "task")

                                    val journeyData = player get JourneyDataObject

                                    if (!journeyData.completedQuests.keys.map { it }.contains(taskName)) {
                                        source.sendFailure(Component.literal("Task '$taskName' is not in your completed quests."))
                                        return@executes 0
                                    }

                                    // Remove the task from completed quests
                                    journeyData.completedQuests.removeIf { it.key == taskName }
                                    player inform "Task '${taskName.toString()}' has been removed from your completed quests.".parseToNative()
                                    return@executes 1
                                }
                        ).build()

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
