package aster.amo.journey.data

import aster.amo.ceremony.data.DataObject
import aster.amo.ceremony.data.DataObjectKey
import aster.amo.ceremony.data.PlayerData
import aster.amo.journey.Journey
import aster.amo.journey.config.ConfigManager
import aster.amo.journey.task.Subtask
import aster.amo.journey.task.Task
import aster.amo.journey.task.TaskRegistry
import aster.amo.journey.utils.inform
import aster.amo.journey.utils.moveToTop
import aster.amo.journey.utils.parseToNative
import aster.amo.journey.utils.toTrimmedString
import com.cobblemon.mod.common.util.asResource
import com.cobblemon.mod.common.util.toVec3d
import eu.pb4.sidebars.api.ScrollableSidebar
import eu.pb4.sidebars.api.Sidebar
import eu.pb4.sidebars.api.SidebarInterface
import eu.pb4.sidebars.api.SidebarUtils
import eu.pb4.sidebars.api.lines.LineBuilder
import eu.pb4.sidebars.api.lines.SidebarLine
import eu.pb4.sidebars.api.lines.SuppliedSidebarLine
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.network.chat.numbers.BlankFormat
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.animal.Parrot
import net.minecraft.world.entity.animal.Pig
import net.minecraft.world.entity.animal.allay.Allay
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import java.util.LinkedHashMap
import java.util.UUID
import kotlin.math.absoluteValue

class JourneyDataObject(player: Player) : DataObject(player) {
    var showZoneBounds: Boolean = false
    var starterPokemon: UUID? = null

    val tasksProgress: LinkedHashMap<ResourceLocation, TaskProgress> = linkedMapOf()

    val activeSubtasksByEvent: LinkedHashMap<String, MutableList<SubtaskProgress>> = linkedMapOf()
    var sidebar: SidebarInterface? = null
    val completedQuests: MutableMap<ResourceLocation, TaskProgress> = mutableMapOf()
    var trackedTaskId: ResourceLocation? = null

    override fun readFromNbt(tag: CompoundTag) {
        tasksProgress.clear()
        val tasksListTag = tag.getList("tasksProgress", 10) // 10 = CompoundTag
        tasksListTag.forEach { element ->
            val compoundTag = element as CompoundTag
            val taskProgress = TaskProgress.fromNbt(compoundTag)
            tasksProgress[taskProgress.taskId] = taskProgress
        }
        if(tag.contains("starterPokemon")) {
            starterPokemon = tag.getUUID("starterPokemon")
        }
        completedQuests.clear()
        val completedQuestsListTag = tag.getList("completedQuests", 10)
        completedQuestsListTag.forEach { element ->
            val compoundTag = element as CompoundTag
            val taskProgress = TaskProgress.fromNbt(compoundTag)
            completedQuests[taskProgress.taskId] = taskProgress
        }
        trackedTaskId = tag.getString("trackedTaskId").asResource()
        showZoneBounds = tag.getBoolean("showZoneBounds")
        rebuildActiveSubtasksIndex()
    }

    override fun writeToNbt(tag: CompoundTag) {
        val tasksListTag = ListTag()
        tasksProgress.values.forEach { taskProgress ->
            tasksListTag.add(taskProgress.toNbt())
        }
        tag.put("tasksProgress", tasksListTag)
        starterPokemon?.let { tag.putUUID("starterPokemon", it) }
        val completedQuestsListTag = ListTag()
        completedQuests.values.forEach { taskProgress ->
            completedQuestsListTag.add(taskProgress.toNbt())
        }
        tag.put("completedQuests", completedQuestsListTag)
        tag.putString("trackedTaskId", trackedTaskId?.toString() ?: "")
        tag.putBoolean("showZoneBounds", showZoneBounds)
    }

    private fun rebuildActiveSubtasksIndex() {
        activeSubtasksByEvent.clear()
        tasksProgress.values.forEach { taskProgress ->
            taskProgress.subtasksProgress.values.forEach { subtaskProgress ->
                val subtask = TaskRegistry.getSubtask(taskProgress.taskId, subtaskProgress.subtaskId)
                if (subtask != null) {
                    val eventName = subtask.event.name
                    activeSubtasksByEvent.computeIfAbsent(eventName) { mutableListOf() }.add(subtaskProgress)
                }
            }
        }
    }

    fun checkAndCompleteTask(taskId: ResourceLocation, subtask: Subtask, subtaskProgress: SubtaskProgress) {
        val taskProgress = tasksProgress[taskId]
        if (taskProgress != null) {
            val task = TaskRegistry.TASKS[taskId]
            if (task != null) {
                val isSequential = task.isSequential

                taskProgress.subtasksProgress.remove(subtask.id)

                activeSubtasksByEvent[subtask.event.name]?.remove(subtaskProgress)

                if (isSequential) {
                    val subtasks = task.tasks
                    val currentIndex = subtasks.indexOfFirst { it.id == subtask.id }
                    val nextIndex = currentIndex + 1
                    if (nextIndex < subtasks.size) {
                        val nextSubtask = subtasks[nextIndex]
                        val nextSubtaskProgress = SubtaskProgress(taskId, nextSubtask.id)
                        taskProgress.subtasksProgress[nextSubtask.id] = nextSubtaskProgress
                        taskProgress.currentSubtaskId = nextSubtask.id
                        activeSubtasksByEvent.computeIfAbsent(nextSubtask.event.name) { mutableListOf() }
                            .add(nextSubtaskProgress)

                    } else {
                        tasksProgress.remove(taskId)
                        task.rewards.forEach { it.parse(player as ServerPlayer) }
                        completedQuests.getOrPut(taskId) { taskProgress }
                        player as ServerPlayer inform "<green>Task ".parseToNative().append(task.name.parseToNative()).append("<green> is complete!".parseToNative())
                    }
                } else {
                    if (taskProgress.subtasksProgress.isEmpty()) {
                        tasksProgress.remove(taskId)
                        task.rewards.forEach { it.parse(player as ServerPlayer) }
                        completedQuests.getOrPut(taskId) { taskProgress }
                        player as ServerPlayer inform "<green>Task ".parseToNative().append(task.name.parseToNative()).append("<green> is complete!".parseToNative())
                    }
                }

                if (tasksProgress.isEmpty()) {
                    SidebarUtils.removeSidebar((player as ServerPlayer).connection, sidebar)
                } else {
                    rebuildSidebar()
                }
            }
        }
    }

    fun addTask(task: Task) {
        val taskId = task.id()
        if (tasksProgress.containsKey(taskId)) {
            player as ServerPlayer inform "<red>Task ".parseToNative().append(task.name.parseToNative()).append("<red> is already active!".parseToNative())
            return
        }

        val taskProgress = if (task.isSequential) {
            val firstSubtask = task.tasks.firstOrNull()
            if (firstSubtask == null) {
                player as ServerPlayer inform "<red>Task ".parseToNative().append(task.name.parseToNative()).append("<red> has no subtasks!".parseToNative())
                return
            }
            val subtaskProgress = SubtaskProgress(taskId, firstSubtask.id)
            val subtasksProgress = mutableMapOf<String, SubtaskProgress>()
            subtasksProgress[firstSubtask.id] = subtaskProgress

            activeSubtasksByEvent.computeIfAbsent(firstSubtask.event.name) { mutableListOf() }
                .add(subtaskProgress)

            TaskProgress(taskId, subtasksProgress, currentSubtaskId = firstSubtask.id)
        } else {
            val subtasksProgress = mutableMapOf<String, SubtaskProgress>()
            task.tasks.forEach { subtask ->
                val subtaskProgress = SubtaskProgress(taskId, subtask.id)
                subtasksProgress[subtask.id] = subtaskProgress

                activeSubtasksByEvent.computeIfAbsent(subtask.event.name) { mutableListOf() }
                    .add(subtaskProgress)
            }
            TaskProgress(taskId, subtasksProgress)
        }
        tasksProgress[taskId] = taskProgress
        rebuildSidebar()
    }

    fun rebuildSidebar() {
        if (sidebar != null) {
            SidebarUtils.removeSidebar((player as ServerPlayer).connection, sidebar)
        }
        val taskSidebar: Sidebar =
            if(player.isShiftKeyDown)
                ScrollableSidebar(ConfigManager.CONFIG.questSidebarTitle.parseToNative(), Sidebar.Priority.MEDIUM, 20)
            else
                Sidebar(ConfigManager.CONFIG.questSidebarTitle.parseToNative(), Sidebar.Priority.MEDIUM)
        taskSidebar.set { builder ->
            addTaskLines(builder)
        }
        taskSidebar.apply {
            this.setPriority(Sidebar.Priority.MEDIUM)
            this.defaultNumberFormat = BlankFormat()
        }
        taskSidebar.defaultNumberFormat = BlankFormat()
        SidebarUtils.addSidebar((player as ServerPlayer).connection, taskSidebar)
        sidebar = taskSidebar
    }

    private fun addTaskLines(builder: LineBuilder) {
        var index = 0
        for (taskID in tasksProgress.values) {
            index++
            if(index >= ConfigManager.CONFIG.maxTasksShown) {
                break
            }
            val task = TaskRegistry.TASKS[taskID.taskId]
            val taskName = task?.name ?: return
            builder.add(SidebarLine.create(0, taskName.parseToNative(), BlankFormat.INSTANCE))
            if(ConfigManager.CONFIG.showDescriptionInSidebar) {
                val firstLine = if (task.description.isNotEmpty()) task.description[0] else ""
                val style = firstLine.parseToNative().style
                val parts = mutableListOf<String>()
                var currentLine = ""
                for (word in firstLine.split(" ")) {
                    if (currentLine.length + word.length > ConfigManager.CONFIG.taskDescriptionMaxLength) {
                        parts.add(currentLine)
                        currentLine = " $word"
                    } else {
                        currentLine += " $word"
                    }
                }
                parts.add(currentLine)
                for (part in parts) {
                    builder.add(
                        SidebarLine.create(
                            0,
                            (" ${ConfigManager.CONFIG.taskSeparatorCharacter} ").parseToNative()
                                .append(part.parseToNative().copy().setStyle(style)),
                            BlankFormat.INSTANCE
                        )
                    )
                }
            }
            addSubtaskLines(taskID, builder)
        }
    }

    private fun addSubtaskLines(taskID: TaskProgress, builder: LineBuilder) {
        val subtasks = tasksProgress[taskID.taskId]?.subtasksProgress?.values ?: emptyList()
        for (subtaskProgress in subtasks) {
            val subtask = TaskRegistry.getSubtask(taskID.taskId, subtaskProgress.subtaskId) ?: continue
            builder.add(
                createTrackingLine(taskID, subtask)
            )
            if(ConfigManager.CONFIG.showDescriptionInSidebar && subtasks.size < 2) {
                val firstLine = if (subtask.description.isNotEmpty()) subtask.description else ""
                val style = firstLine.parseToNative().style
                val parts = mutableListOf<String>()
                var currentLine = ""
                for (word in firstLine.split(" ")) {
                    if (currentLine.length + word.length > ConfigManager.CONFIG.subtaskDescriptionMaxLength) {
                        parts.add(currentLine)
                        currentLine = word
                    } else {
                        currentLine += " $word"
                    }
                }
                parts.add(currentLine)
                for (part in parts) {
                    builder.add(
                        SidebarLine.create(
                            0,
                            ("    ${ConfigManager.CONFIG.subtaskDescriptionSeparator} ").parseToNative()
                                .append(part.parseToNative().copy().setStyle(style)),
                            BlankFormat.INSTANCE
                        )
                    )
                }
            }
        }
    }

    private fun createTrackingLine(
        taskID: TaskProgress,
        subtask: Subtask
    ) = SuppliedSidebarLine(0,
        { player ->
            val subtaskProgress = taskID.subtasksProgress[subtask.id]
            val subtaskName = subtask.name
            val progress = subtaskProgress?.progress ?: 0.0
            val target = subtask.target
            val direction = if (subtask.location == null) "" else {
                if (trackedTaskId != taskID.taskId) {
                    ""
                } else {
                    val eightDirectionalArrows = arrayOf("↑", "↗", "→", "↘", "↓", "↙", "←", "↖")
                    val playerPos = player.position()
                    val subtaskPos = subtask.location

                    val dx = subtaskPos.x.toDouble() - playerPos.x
                    val dz = subtaskPos.z.toDouble() - playerPos.z

                    val angleToSubtaskDegrees = (Math.toDegrees(Math.atan2(-dx, dz)) + 360) % 360
                    val playerYawDegrees = (player.yRot + 360) % 360
                    val angleDifferenceDegrees =
                        (angleToSubtaskDegrees - playerYawDegrees + 360) % 360
                    val index = ((angleDifferenceDegrees + 22.5) / 45).toInt() % 8
                    val distance = ((playerPos.distanceTo(
                        Vec3(
                            subtaskPos.x.toDouble(),
                            subtaskPos.y.toDouble(),
                            subtaskPos.z.toDouble()
                        )
                    ) * 10.0).toInt()) / 10.0

                    "<red>${eightDirectionalArrows[index]} <gold>$distance</gold> "
                }
            }
            return@SuppliedSidebarLine ("  ${ConfigManager.CONFIG.subtaskSeparatorCharacter} $subtaskName").parseToNative()
                .copy()
                .append(": ${progress.toTrimmedString()}/${target.toTrimmedString()} $direction".parseToNative())
        }, { player ->
            return@SuppliedSidebarLine BlankFormat.INSTANCE
        }
    )

    fun hasTask(taskId: ResourceLocation): Boolean {
        return tasksProgress.containsKey(taskId)
    }

    fun removeTask(taskId: ResourceLocation) {
        val taskProgress = tasksProgress.remove(taskId)
        if (taskProgress != null) {
            taskProgress.subtasksProgress.values.forEach { subtaskProgress ->
                val subtask = TaskRegistry.getSubtask(taskId, subtaskProgress.subtaskId)
                if (subtask != null) {
                    activeSubtasksByEvent[subtask.event.name]?.remove(subtaskProgress)
                }
            }
        }
        if (tasksProgress.isEmpty()) {
            SidebarUtils.removeSidebar((player as ServerPlayer).connection, sidebar)
        } else {
            rebuildSidebar()
        }
    }

    fun getActiveSubtasks(eventName: String): List<Triple<ResourceLocation, Subtask, SubtaskProgress>> {
        return activeSubtasksByEvent[eventName]?.mapNotNull { subtaskProgress ->
            val taskId = subtaskProgress.taskId
            val subtask = TaskRegistry.getSubtask(taskId, subtaskProgress.subtaskId)
            if (subtask != null) {
                Triple(taskId, subtask, subtaskProgress)
            } else {
                null
            }
        } ?: emptyList()
    }

    fun setTrackedTask(taskId: ResourceLocation?) {
        trackedTaskId = taskId
        if (taskId != null) {
            tasksProgress.moveToTop(taskId)
        }
    }

    // Method to get the tracked task
    fun getTrackedTask(): TaskProgress? {
        val taskId = trackedTaskId ?: return null
        return tasksProgress[taskId]
    }

    companion object Key : DataObjectKey<JourneyDataObject> {
        val RL: ResourceLocation = ResourceLocation.fromNamespaceAndPath("journey", "journey_data_object")

        init {
            PlayerData.setupData {
                PlayerData.registerData(RL, JourneyDataObject::class.java)
            }
        }
    }
}
