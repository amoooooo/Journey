package aster.amo.journey.data

import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation

data class TaskProgress(
    val taskId: ResourceLocation,
    val subtasksProgress: MutableMap<String, SubtaskProgress> = mutableMapOf(),
    var currentSubtaskId: String? = null,
    var completedTime: Long = -1L,
    var whenToReset: Long = -1L
) {

    fun reset() {
        subtasksProgress.clear()
        completedTime = -1L
        currentSubtaskId = null
        whenToReset = -1L
    }

    fun toNbt(): CompoundTag {
        val tag = CompoundTag()
        tag.putString("taskId", taskId.toString())
        tag.putString("currentSubtaskId", currentSubtaskId ?: "")
        tag.putLong("completedTime", completedTime)
        val subtasksTag = CompoundTag()
        subtasksProgress.values.forEach { subtaskProgress ->
            subtasksTag.put(subtaskProgress.subtaskId, subtaskProgress.toNbt())
        }
        tag.put("subtasksProgress", subtasksTag)
        tag.putLong("whenToReset", whenToReset)
        return tag
    }

    companion object {
        fun fromNbt(tag: CompoundTag): TaskProgress {
            val taskId = ResourceLocation.parse(tag.getString("taskId"))
            val currentSubtaskId = tag.getString("currentSubtaskId").takeIf { it.isNotEmpty() }
            val completedTime = tag.getLong("completedTime")
            val subtasksProgress = mutableMapOf<String, SubtaskProgress>()
            val subtasksTag = tag.getCompound("subtasksProgress")
            subtasksTag.allKeys.forEach { subtaskId ->
                val subtaskProgress = SubtaskProgress.fromNbt(subtasksTag.getCompound(subtaskId))
                subtasksProgress[subtaskId] = subtaskProgress
            }
            val whenToReset = tag.getLong("whenToReset")
            return TaskProgress(taskId, subtasksProgress, currentSubtaskId, completedTime, whenToReset)
        }
    }
}
