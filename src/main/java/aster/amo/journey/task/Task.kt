package aster.amo.journey.task

import net.minecraft.resources.ResourceLocation
import org.spongepowered.include.com.google.gson.annotations.SerializedName
import aster.amo.journey.task.reward.Reward
import com.bedrockk.molang.runtime.MoLangRuntime
import com.cobblemon.mod.common.api.molang.MoLangFunctions
import com.cobblemon.mod.common.api.molang.MoLangFunctions.setup
import com.cobblemon.mod.common.util.asExpressionLike
import com.cobblemon.mod.common.util.resolveBoolean
import net.minecraft.server.level.ServerPlayer

class Task(
    val name: String = "",
    val description: List<String> = emptyList(),
    @SerializedName("sequential") val isSequential: Boolean = false,
    @SerializedName("start_requirement") val startRequirement: String = "",
    val rewards: List<Reward> = emptyList(),
    val icon: Icon = Icon(),
    @SerializedName("repeat_type") val repeatType: RepeatType = RepeatType.NONE,
    @SerializedName("repeat_interval") val repeatInterval: Int = 0,
    @SerializedName("repeat_limit") val repeatLimit: Int = 0,
    @SerializedName("starter_npc") val starterNPC: String = "",
    val tasks: ArrayDeque<Subtask> = ArrayDeque<Subtask>(),
    val script: String = ""
) {

    fun id(): ResourceLocation = TaskRegistry.TASKS.keys.first { TaskRegistry.TASKS[it] == this }

    fun canStart(player: ServerPlayer): Boolean {
        val runtime = MoLangRuntime().setup()
        runtime.environment.query.addFunction("player") { MoLangFunctions.playerFunctions.flatMap { it.invoke(player).values } }
        return runtime.resolveBoolean(startRequirement.asExpressionLike())
    }

    companion object {
        val TASKS: MutableMap<ResourceLocation, Task> = mutableMapOf()
    }
}