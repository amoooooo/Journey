package aster.amo.journey.task.reward

import aster.amo.journey.utils.MolangUtils
import com.bedrockk.molang.runtime.MoLangRuntime
import com.cobblemon.mod.common.api.molang.MoLangFunctions.setup
import com.cobblemon.mod.common.util.asExpressionLike
import com.cobblemon.mod.common.util.resolve
import net.minecraft.server.level.ServerPlayer

class ScriptReward(
    private val scripts: List<String>
) : Reward {
    override fun parse(player: ServerPlayer) {
        val runtime = MoLangRuntime().setup()
        MolangUtils.setupPlayerStructs(runtime.environment.query, player)
        scripts.forEach { script ->
            runtime.resolve(script.asExpressionLike())
        }
    }
}