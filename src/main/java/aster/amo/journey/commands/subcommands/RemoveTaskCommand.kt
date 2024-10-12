package aster.amo.journey.commands.subcommands

import aster.amo.ceremony.utils.extension.get
import aster.amo.journey.Journey
import aster.amo.journey.data.JourneyDataObject
import aster.amo.journey.utils.SubCommand
import aster.amo.journey.utils.inform
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import com.mojang.brigadier.tree.LiteralCommandNode

import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.ChatFormatting
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.GameProfileArgument
import net.minecraft.commands.arguments.ResourceLocationArgument
import net.minecraft.server.level.ServerPlayer
import net.minecraft.resources.ResourceLocation
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.network.chat.Component
import java.util.concurrent.CompletableFuture

class RemoveTaskCommand : SubCommand {
    override fun build(): LiteralCommandNode<CommandSourceStack> {
        return Commands.literal("removetask")
            .then(
                Commands.argument("player", GameProfileArgument.gameProfile())
                    .then(
                        Commands.argument("task_id", ResourceLocationArgument.id())
                            .suggests { context, builder ->
                                suggestActiveTasks(context, builder)
                            }
                            .executes(Companion::removeTask)
                    )
            )
            .build()
    }

    companion object {
        private fun suggestActiveTasks(
            context: CommandContext<CommandSourceStack>,
            builder: SuggestionsBuilder
        ): CompletableFuture<Suggestions> {
            try {
                val gameProfiles = GameProfileArgument.getGameProfiles(context, "player")
                val server = context.source.server

                if (gameProfiles.size == 1) {
                    val gameProfile = gameProfiles.first()
                    val player = server.playerList.getPlayer(gameProfile.id)
                    if (player != null) {
                        val data = player get JourneyDataObject
                        val activeTaskIds = data.tasksProgress.keys.map { it.toString() }
                        return SharedSuggestionProvider.suggest(activeTaskIds, builder)
                    }
                }
            } catch (_: Exception) {
            }
            return SharedSuggestionProvider.suggest(emptyList<String>(), builder)
        }

        fun removeTask(ctx: CommandContext<CommandSourceStack>): Int {
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

            val taskId = ResourceLocationArgument.getId(ctx, "task_id")

            var successCount = 0
            players.forEach { player ->
                val data = player get JourneyDataObject
                if (data.hasTask(taskId)) {
                    data.removeTask(taskId)
                    player inform Component.literal("Removed task: ${taskId}")
                    successCount++
                } else {
                    player inform Component.literal("Task not found: ${taskId}").withStyle { it.withColor(ChatFormatting.RED) }
                }
            }

            return if (successCount > 0) 1 else 0
        }
    }
}
