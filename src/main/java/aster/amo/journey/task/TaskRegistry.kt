package aster.amo.journey.task

import net.minecraft.resources.ResourceLocation

object TaskRegistry {
    val TASKS: MutableMap<ResourceLocation, Task> = mutableMapOf()
    val EVENT_TASKS: MutableMap<String, MutableList<Task>> = mutableMapOf()

    fun registerTask(id: ResourceLocation, task: Task) {
        TASKS[id] = task
        task.tasks.forEach { subtask ->
            EVENT_TASKS.computeIfAbsent(subtask.event.name) { mutableListOf() }.add(task)
        }
    }

    fun getTasksByEvent(eventName: String): List<Task> {
        return EVENT_TASKS[eventName] ?: emptyList()
    }

    fun getSubtask(taskId: ResourceLocation, subtaskId: String): Subtask? {
        val task = TASKS[taskId]
        return task?.tasks?.find { it.id == subtaskId }
    }

    fun clear() {
        TASKS.clear()
        EVENT_TASKS.clear()
    }
}