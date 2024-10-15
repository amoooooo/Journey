package aster.amo.journey.task

import aster.amo.journey.data.JourneyDataObject
import net.minecraft.resources.ResourceLocation
import java.util.UUID

class TaskStateEvaluator() {
    var available: () -> Unit = {}
    var subtaskTarget: () -> Unit = {}
    var handIn: () -> Unit = {}

    fun onAvailable(action: () -> Unit): TaskStateEvaluator {
        available = action
        return this
    }

    fun onSubtaskTarget(action: () -> Unit): TaskStateEvaluator {
        subtaskTarget = action
        return this
    }

    fun toHandIn(action: () -> Unit): TaskStateEvaluator {
        handIn = action
        return this
    }

    fun evaluate(id: ResourceLocation, uuid: UUID, data: JourneyDataObject, taskSource: TaskSource) {
        val isAvailable = !data.completedQuests.keys.contains(id) && !data.tasksProgress.contains(id) && taskSource.tasks[id.toString()]?.source == true
        var isSubtaskTarget = false
        val task = TaskRegistry.TASKS[id]
        if(task != null) {
            if(task.isSequential) {
                val progress = data.tasksProgress[id]
                if(progress != null && progress.currentSubtaskId != null) {
                    val subtask = TaskRegistry.getSubtask(id, progress.currentSubtaskId!!)
                    if(subtask != null) {
                        isSubtaskTarget = subtask.event.name == "ENTITY_INTERACT" && subtask.eventData.has("uuid") && subtask.eventData["uuid"].asString == uuid.toString()
                    }
                }
            } else {
                val progress = data.tasksProgress[id]
                if(progress != null) {
                    progress.subtasksProgress.forEach { (subtaskId, subtaskProgress) ->
                        val subtask = TaskRegistry.getSubtask(id, subtaskId)
                        if(subtask != null) {
                            if(subtask.event.name == "ENTITY_INTERACT" && subtask.eventData.has("uuid") && subtask.eventData["uuid"].asString == uuid.toString()) {
                                isSubtaskTarget = true
                            }
                        }
                    }
                }
            }

        }
        var isHandIn = false
        if(isSubtaskTarget && task != null) {
            val progress = data.tasksProgress[id]
            if(progress != null) {
                if(task.isSequential) {
                    val subtasks = task.tasks
                    val lastSubtask = subtasks.last()
                    isHandIn = progress.currentSubtaskId == lastSubtask.id
                } else {
                    val subtasks = task.tasks
                    val completedSubtasks = task.tasks.filter { subtask ->
                        val subtaskProgress = progress.subtasksProgress[subtask.id]
                        subtaskProgress != null && subtaskProgress.progress >= subtask.target
                    }
                    val lastSubtask = subtasks.subtract(completedSubtasks.toSet()).firstOrNull()
                    if(lastSubtask != null) {
                        // check if its an entity interact event and the uuid matches
                        val subtask = TaskRegistry.getSubtask(id, lastSubtask.id)
                        if(subtask != null) {
                            if(subtask.event.name == "ENTITY_INTERACT" && subtask.eventData.has("uuid") && subtask.eventData["uuid"].asString == uuid.toString()) {
                                isHandIn = true
                            }
                        }
                    }
                }
            }
        } else {
            isHandIn =false
        }
        if(isHandIn) handIn()
        else if (isSubtaskTarget) subtaskTarget()
        else if(isAvailable) available()
    }
}