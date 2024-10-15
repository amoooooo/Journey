package aster.amo.journey.commands.subcommands

import aster.amo.ceremony.utils.extension.get
import aster.amo.journey.Journey
import aster.amo.journey.data.JourneyDataObject
import aster.amo.journey.task.TaskSource
import aster.amo.journey.utils.SubCommand
import aster.amo.journey.utils.inform
import aster.amo.journey.utils.parseToNative
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.LiteralCommandNode
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.SharedSuggestionProvider

class FlagCommand : SubCommand {
    override fun build(): LiteralCommandNode<CommandSourceStack> {
        return Commands.literal("flag")
            .then(
                Commands.literal("add")
                    .then(
                        Commands.argument("flag", StringArgumentType.string())
                            .executes(Companion::addFlag)
                    )
            )
            .then(
                Commands.literal("remove")
                    .then(
                        Commands.argument("flag", StringArgumentType.string())
                            .suggests { context, builder ->
                                val source = context.source
                                val player = source.playerOrException
                                val data = player get JourneyDataObject
                                SharedSuggestionProvider.suggest(data.flags, builder)
                            }
                            .executes(Companion::removeFlag)
                    )
            )
            .build()
    }

    companion object {
        /**
         * Suggestion provider that suggests flag names based on the player's current flags.
         */
        private suspend fun suggestExistingFlags(
            context: CommandContext<CommandSourceStack>,
            builder: com.mojang.brigadier.suggestion.SuggestionsBuilder
        ): java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> {
            val source = context.source
            val player = source.playerOrException
            val data = player get JourneyDataObject

            return SharedSuggestionProvider.suggest(data.flags, builder)
        }

        /**
         * Executes the `/journey flag add <flagName>` command.
         */
        fun addFlag(ctx: CommandContext<CommandSourceStack>): Int {
            val source = ctx.source
            val player = source.playerOrException

            val flagName = StringArgumentType.getString(ctx, "flag")
            val journeyData = player get JourneyDataObject

            if (journeyData.flags.contains(flagName)) {
                player inform "<red>You already have the flag '$flagName'.</red>".parseToNative()
                return 0
            }

            journeyData.flags.add(flagName)
            player inform "<green>Flag '$flagName' has been added.</green>".parseToNative()
            Journey.LOGGER.info("Player ${player.getName().string} added flag: $flagName")
            return 1
        }

        /**
         * Executes the `/journey flag remove <flagName>` command.
         */
        fun removeFlag(ctx: CommandContext<CommandSourceStack>): Int {
            val source = ctx.source
            val player = source.playerOrException

            val flagName = StringArgumentType.getString(ctx, "flag")
            val journeyData = player get JourneyDataObject

            if (!journeyData.flags.contains(flagName)) {
                player inform "<red>You do not have the flag '$flagName'.</red>".parseToNative()
                return 0
            }

            journeyData.flags.remove(flagName)
            player inform "<yellow>Flag '$flagName' has been removed.</yellow>".parseToNative()
            Journey.LOGGER.info("Player ${player.getName().string} removed flag: $flagName")
            return 1
        }
    }
}
