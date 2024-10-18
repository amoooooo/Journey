package aster.amo.journey.task.reward

import aster.amo.journey.timeline.Timeline
import aster.amo.journey.utils.MolangUtils
import com.bedrockk.molang.runtime.MoLangRuntime
import com.cobblemon.mod.common.api.molang.MoLangFunctions.setup
import com.cobblemon.mod.common.util.asExpressionLike
import com.cobblemon.mod.common.util.resolve
import net.minecraft.server.level.ServerPlayer

class TimelineReward(
    private val timeline: String = ""
) : Reward {
    override fun parse(player: ServerPlayer) {
        Timeline.TIMELINES[timeline]?.launch(player)
    }
}