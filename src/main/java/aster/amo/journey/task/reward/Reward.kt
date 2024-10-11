package aster.amo.journey.task.reward

import net.minecraft.server.level.ServerPlayer

interface Reward {
    fun parse(player: ServerPlayer)
}
