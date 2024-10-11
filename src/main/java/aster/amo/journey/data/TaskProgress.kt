package aster.amo.journey.data

import aster.amo.journey.task.TaskRegistry
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation

class TaskProgress(
    val taskId: ResourceLocation,
    val subtasksProgress: MutableMap<String, SubtaskProgress> = mutableMapOf(),
    var currentSubtaskId: String? = null
) {
    fun reset() {
        subtasksProgress.clear()
    }

    fun toNbt(): CompoundTag {
        val tag = CompoundTag()
        tag.putString("taskId", taskId.toString())
        tag.putString("currentSubtaskId", currentSubtaskId ?: "")
        val subtasksTag = CompoundTag()
        val task = TaskRegistry.TASKS[taskId]

        if (task != null && task.isSequential) {
            if (currentSubtaskId != null) {
                val subtaskProgress = subtasksProgress[currentSubtaskId]
                if (subtaskProgress != null) {
                    currentSubtaskId?.let { subtasksTag.put(it, subtaskProgress.toNbt()) }
                }
            }
        } else {
            subtasksProgress.forEach { (subtaskId, subtaskProgress) ->
                subtasksTag.put(subtaskId, subtaskProgress.toNbt())
            }
        }

        tag.put("subtasksProgress", subtasksTag)
        return tag
    }


    companion object {
        fun fromNbt(tag: CompoundTag): TaskProgress {
            val taskId = ResourceLocation.parse(tag.getString("taskId"))
            val currentSubtaskId = tag.getString("currentSubtaskId").takeIf { it.isNotEmpty() }
            val subtasksProgress = mutableMapOf<String, SubtaskProgress>()
            val subtasksTag = tag.getCompound("subtasksProgress")
            val task = TaskRegistry.TASKS[taskId]

            if (task != null && task.isSequential) {
                // For sequential tasks, load only the current subtask
                if (currentSubtaskId != null && subtasksTag.contains(currentSubtaskId)) {
                    val subtaskProgress = SubtaskProgress.fromNbt(subtasksTag.getCompound(currentSubtaskId))
                    subtasksProgress[currentSubtaskId] = subtaskProgress
                }
            } else {
                // For non-sequential tasks, load all subtasks
                subtasksTag.allKeys.forEach { subtaskId ->
                    val subtaskProgress = SubtaskProgress.fromNbt(subtasksTag.getCompound(subtaskId))
                    subtasksProgress[subtaskId] = subtaskProgress
                }
            }

            return TaskProgress(taskId, subtasksProgress, currentSubtaskId)
        }
    }

}
