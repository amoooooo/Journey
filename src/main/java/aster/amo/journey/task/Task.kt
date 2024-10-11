package aster.amo.journey.task

import net.minecraft.resources.ResourceLocation
import org.spongepowered.include.com.google.gson.annotations.SerializedName
import aster.amo.journey.task.reward.Reward

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
    val tasks: ArrayDeque<Subtask> = ArrayDeque<Subtask>()
) {

    fun id(): ResourceLocation = TaskRegistry.TASKS.keys.first { TaskRegistry.TASKS[it] == this }

    companion object {
        val TASKS: MutableMap<ResourceLocation, Task> = mutableMapOf()
    }
}