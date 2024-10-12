package aster.amo.journey.task

import aster.amo.ceremony.utils.extension.get
import com.cobblemon.mod.common.util.asResource
import net.minecraft.server.level.ServerPlayer
import org.spongepowered.include.com.google.gson.annotations.SerializedName
import aster.amo.journey.data.JourneyDataObject
import aster.amo.journey.task.event.JourneyEvent
import aster.amo.journey.task.event.JourneyEvents
import aster.amo.journey.task.reward.Reward
import com.cobblemon.mod.common.api.molang.ExpressionLike
import com.cobblemon.mod.common.util.asExpressionLike
import com.google.gson.JsonObject
import org.joml.Vector3i

class Subtask(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val event: JourneyEvent = JourneyEvents.BATTLE_VICTORY,
    @SerializedName("event_data") val eventData: JsonObject = JsonObject(),
    val filter: String = "",
    val target: Double = 1.0,
    val rewards: List<Reward> = emptyList(),
    val location: Vector3i? = null
) {
    fun complete(player: ServerPlayer) {
        rewards.forEach { it.parse(player) }
        onComplete(player)
    }

    private fun onComplete(player: ServerPlayer) {
        val data = player get JourneyDataObject
        data.activeSubtasksByEvent[event.name]?.removeIf { it.subtaskId == id }
    }

    private var cachedFilterExpression: ExpressionLike? = null

    fun getOrParseFilterExpression(): ExpressionLike {
        if (cachedFilterExpression == null) {
            cachedFilterExpression = filter.asExpressionLike()
        }
        return cachedFilterExpression!!
    }


}
