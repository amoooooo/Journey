package aster.amo.journey.task

import aster.amo.ceremony.utils.extension.get
import com.cobblemon.mod.common.util.asResource
import kotlinx.serialization.json.JsonObject
import net.minecraft.server.level.ServerPlayer
import org.spongepowered.include.com.google.gson.annotations.SerializedName
import aster.amo.journey.data.JourneyDataObject
import aster.amo.journey.task.event.JourneyEvent
import aster.amo.journey.task.event.JourneyEvents
import aster.amo.journey.task.reward.Reward
import com.cobblemon.mod.common.api.molang.ExpressionLike
import com.cobblemon.mod.common.util.asExpressionLike
import org.joml.Vector3i

class Subtask(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val event: JourneyEvent = JourneyEvents.BATTLE_VICTORY,
    @SerializedName("event_data") val eventData: JsonObject = JsonObject(mapOf()),
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
        // Remove the subtask from activeSubtasksByEvent
        data.activeSubtasksByEvent[event.name]?.removeIf { it.subtaskId == id }
        // Additional completion logic...
    }

    private var cachedFilterExpression: ExpressionLike? = null

    fun getOrParseFilterExpression(): ExpressionLike {
        if (cachedFilterExpression == null) {
            cachedFilterExpression = filter.asExpressionLike()
        }
        return cachedFilterExpression!!
    }
}
