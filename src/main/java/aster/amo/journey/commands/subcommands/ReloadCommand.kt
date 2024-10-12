package aster.amo.journey.commands.subcommands

import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.LiteralCommandNode
import aster.amo.journey.Journey
import aster.amo.journey.utils.SubCommand
import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.permission.PermissionValidator

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands

class ReloadCommand : SubCommand {
    override fun build(): LiteralCommandNode<CommandSourceStack> {
        return Commands.literal("reload")
            .executes(Companion::reload)
            .build()
    }

    companion object {
        fun reload(ctx: CommandContext<CommandSourceStack>): Int {
            Journey.INSTANCE.reload()
            ctx.source.sendMessage(Component.text("Reloaded ${Journey.MOD_NAME}!").color(NamedTextColor.GREEN))
            return 1
        }
    }
}
