package aster.amo.journey.task.reward

import net.minecraft.server.level.ServerPlayer

class CurrencyReward(
    val currency: String,
    val amount: Int
) : Reward {
    override fun parse(player: ServerPlayer) {
//        ImpactorModule.IMPACTOR_ECONOMY_SERVICE.deposit(player, amount.toDouble(), currency)
    }
}