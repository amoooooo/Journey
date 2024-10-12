package aster.amo.journey.utils

import aster.amo.ceremony.utils.extension.get
import aster.amo.journey.Journey
import aster.amo.journey.data.JourneyDataObject
import aster.amo.journey.task.TaskRegistry
import com.bedrockk.molang.runtime.MoParams
import com.bedrockk.molang.runtime.struct.QueryStruct
import com.bedrockk.molang.runtime.value.DoubleValue
import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.molang.MoLangFunctions
import com.cobblemon.mod.common.util.asResource
import com.cobblemon.mod.common.util.asUUID
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack

object JourneyMolang {
    fun setupPlayerExtensions() {
        MoLangFunctions.playerFunctions.add { player ->
            val map = hashMapOf<String, java.util.function.Function<MoParams, Any>>()
            map.put("has_completed_task") { params ->
                val taskName = params.getString(0)
                return@put DoubleValue(if((player get JourneyDataObject).completedQuests.contains(taskName.asResource())) 1.0 else 0.0)
            }
            map.put("has_completed_subtask") { params ->
                val taskName = params.getStringOrNull(0)?.asResource() ?: return@put DoubleValue(0.0)
                val subtaskName = params.getStringOrNull(1) ?: return@put DoubleValue(0.0)
                val subtask = TaskRegistry.getSubtask(taskName, subtaskName) ?: return@put DoubleValue(0.0)
                val journeyData = player get JourneyDataObject
                val hasCompleted = isTaskCompleted(journeyData, taskName, subtaskName, subtask.target)
                DoubleValue(if (hasCompleted) 1.0 else 0.0)
            }
            map.put("start_task") { params ->
                val taskName = params.getStringOrNull(0)?.asResource() ?: return@put DoubleValue(0.0)
                val journeyData = player get JourneyDataObject
                val task = TaskRegistry.TASKS[taskName] ?: return@put DoubleValue(0.0)
                journeyData.addTask(task)
                DoubleValue(1.0)
            }
            map.put("has_item") { params ->
                val itemId = params.getString(0)
                val hasItem = player.inventory.contains(
                    ItemStack(
                        BuiltInRegistries.ITEM.get(
                            ResourceLocation.parse(itemId)
                        )
                    )
                )
                DoubleValue(if (hasItem) 1.0 else 0.0)
            }
            map.put("pokedex") { params ->
                Cobblemon.playerDataManager.getPokedexData(player as ServerPlayer).struct
            }
            map.put("starter_pokemon") { params ->
                val data = player get JourneyDataObject
                data.starterPokemon?.let {
                    Cobblemon.storage.getParty(player as ServerPlayer)[it]
                } ?: QueryStruct(hashMapOf())
            }
            map.put("is_in_zone") { params ->
                val zoneUUID = params.getString(0).asUUID
                val zone = Journey.INSTANCE.zoneManager.findZoneContaining(player.onPos) ?: return@put DoubleValue(0.0)
                DoubleValue(if (zone.uuid == zoneUUID) 1.0 else 0.0)
            }
            map
        }
    }


    /**
     * Determines whether the task or subtask has been completed.
     *
     * @param journeyData The player's journey data.
     * @param taskName The name of the main task.
     * @param subtaskName The name of the subtask.
     * @param target The target value to consider the subtask as completed.
     * @return True if the task or subtask is completed, false otherwise.
     */
    fun isTaskCompleted(
        journeyData: JourneyDataObject,
        taskName: ResourceLocation,
        subtaskName: String,
        target: Double
    ): Boolean {
        // Check if the main task is completed
        if (journeyData.completedQuests.contains(taskName)) {
            return true
        }

        // Find the task progress that contains the subtask
        val subtaskProgress = journeyData.tasksProgress.values
            .mapNotNull { it.subtasksProgress[subtaskName] }
            .firstOrNull()

        // Check if the subtask progress meets or exceeds the target
        return subtaskProgress?.progress?.let { it >= target } ?: false
    }
}
/**
 * Extension function to safely retrieve a string from params.
 * Returns null if the index is out of bounds or the value is not a string.
 */
fun MoParams.getStringOrNull(index: Int): String? {
    return try {
        this.getString(index) ?: ""
    } catch (e: Exception) {
        null
    }
}
