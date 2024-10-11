package aster.amo.journey.task.reward

import net.minecraft.server.level.ServerPlayer

class CommandReward(
    private val command: String
) : Reward {
    override fun parse(player: ServerPlayer) {
        player.server.execute {
            player.server.commands.dispatcher.execute(command.replace("{player}", player.name.string), player.server.createCommandSourceStack())
        }
    }
}