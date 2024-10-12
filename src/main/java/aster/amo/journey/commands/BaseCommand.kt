package aster.amo.journey.commands

import aster.amo.journey.Journey
import aster.amo.journey.commands.subcommands.*
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.tree.LiteralCommandNode

import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands

class BaseCommand {
    private val aliases = listOf("journey")

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        val rootCommands: List<LiteralCommandNode<CommandSourceStack>> = aliases.map {
            Commands.literal(it)
                .build()
        }

        val subCommands: List<LiteralCommandNode<CommandSourceStack>> = listOf(
            ReloadCommand().build(),
            DebugCommand().build(),
            StartTaskCommand().build(),
            RemoveTaskCommand().build(),
            TrackTaskCommand().build(),
            RemoveCompletedTaskCommand().build(),
            ZoneCommand().build()
        )
        rootCommands.forEach { root ->
            subCommands.forEach { sub -> root.addChild(sub) }
            dispatcher.root.addChild(root)
        }

        dispatcher.root.addChild(TrackTaskCommand().build())
        dispatcher.root.addChild(ZoneCommand().build())
    }
}
