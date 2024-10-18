package aster.amo.journey.utils

import aster.amo.ceremony.utils.extension.get
import aster.amo.journey.Journey
import aster.amo.journey.data.JourneyDataObject
import aster.amo.journey.task.TaskRegistry
import aster.amo.journey.timeline.Timeline
import com.bedrockk.molang.runtime.MoParams
import com.bedrockk.molang.runtime.struct.QueryStruct
import com.bedrockk.molang.runtime.value.DoubleValue
import com.bedrockk.molang.runtime.value.StringValue
import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.CobblemonNetwork
import com.cobblemon.mod.common.api.molang.MoLangFunctions
import com.cobblemon.mod.common.api.pokemon.stats.Stats
import com.cobblemon.mod.common.net.messages.client.animation.PlayPosableAnimationPacket
import com.cobblemon.mod.common.net.messages.client.effect.SpawnSnowstormEntityParticlePacket
import com.cobblemon.mod.common.net.messages.client.effect.SpawnSnowstormParticlePacket
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.pokemon.Species
import com.cobblemon.mod.common.util.asResource
import com.cobblemon.mod.common.util.asUUID
import com.cobblemon.mod.common.util.getDoubleOrNull
import kotlinx.coroutines.DelicateCoroutinesApi
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3

object JourneyMolang {

    @OptIn(DelicateCoroutinesApi::class)
    fun setupWorldExtensions() {
        MoLangFunctions.worldFunctions.apply {
            put("snowstorm_particle") { params ->
                val particle = params.getString(0).asResource()
                val x = params.getDouble(1)
                val y = params.getDouble(2)
                val z = params.getDouble(3)
                val player = params.getStringOrNull(4)
                val pos = Vec3(x, y, z)
                val particlePacket = SpawnSnowstormParticlePacket(particle, pos)
                if (player == null) {
                    CobblemonNetwork.sendToAllPlayers(particlePacket)
                } else {
                    val serverPlayer = player.asUUID?.let { Journey.INSTANCE.server?.playerList?.getPlayer(it) }
                        ?: return@put DoubleValue(0.0)
                    CobblemonNetwork.sendPacketToPlayer(serverPlayer, particlePacket)
                }
            }
            put("show_zone_for_player") { params ->
            }
        }
    }

    fun setupNPCExtensions() {
        MoLangFunctions.npcFunctions.add { npc ->
            val map = hashMapOf<String, java.util.function.Function<MoParams, Any>>()
            map.put("play_animation") { params ->
                val animation = params.getString(0)
                val player = params.getString(1) ?: null
                if (player == null) {
                    CobblemonNetwork.sendToAllPlayers(PlayPosableAnimationPacket(npc.id, setOf(animation), emptyList()))
                } else {
                    val serverPlayer = player.asUUID?.let { Journey.INSTANCE.server?.playerList?.getPlayer(it) }
                        ?: return@put DoubleValue(0.0)
                    CobblemonNetwork.sendPacketToPlayer(
                        serverPlayer,
                        PlayPosableAnimationPacket(npc.id, setOf(animation), emptyList())
                    )
                }
                return@put DoubleValue(1.0)
            }
            map.put("say") { params ->
                val message = params.getString(0)
                val player = params.getStringOrNull(1)
                if (player == null) {
                    val nearbyPlayers =
                        npc.level().players().filter { it.distanceTo(npc) < 64.0 }.map { it as ServerPlayer }
                    val editedMessage = message.replace("%npc%", npc.name.toString())
                    nearbyPlayers.forEach { serverPlayer: ServerPlayer -> serverPlayer.sendSystemMessage(editedMessage.parseToNative()) }
                } else {
                    val serverPlayer = player.asUUID?.let { Journey.INSTANCE.server?.playerList?.getPlayer(it) }
                        ?: return@put DoubleValue(0.0)
                    val editedMessage = message.replace("%npc%", npc.name.toString())
                    serverPlayer.sendSystemMessage(editedMessage.parseToNative())
                }
            }
            map.put("snowstorm_entity_particle") { params ->
                val particle = params.getString(0).asResource()
                val locator = params.getString(1)
                val player = params.getStringOrNull(2)
                val particlePacket = SpawnSnowstormEntityParticlePacket(particle, npc.id, listOf(locator))
                if (player == null) {
                    CobblemonNetwork.sendToAllPlayers(particlePacket)
                } else {
                    val serverPlayer = player.asUUID?.let { Journey.INSTANCE.server?.playerList?.getPlayer(it) }
                        ?: return@put DoubleValue(0.0)
                    CobblemonNetwork.sendPacketToPlayer(serverPlayer, particlePacket)
                }
            }
            map.put("snowstorm_particle") { params ->
                val particle = params.getString(0).asResource()
                val x = params.getDouble(1)
                val y = params.getDouble(2)
                val z = params.getDouble(3)
                val player = params.getStringOrNull(4)
                val pos = Vec3(x, y, z).add(npc.position())
                val particlePacket = SpawnSnowstormParticlePacket(particle, pos)
                if (player == null) {
                    if (npc.level() == null) return@put DoubleValue(0.0)
                    CobblemonNetwork.sendToAllPlayers(particlePacket)
                } else {
                    val serverPlayer = player.asUUID?.let { Journey.INSTANCE.server?.playerList?.getPlayer(it) }
                        ?: return@put DoubleValue(0.0)
                    CobblemonNetwork.sendPacketToPlayer(serverPlayer, particlePacket)
                }
            }
            map.put("distance_to_player") { params ->
                val player = params.getString(0)
                val serverPlayer =
                    player.asUUID?.let { Journey.INSTANCE.server?.playerList?.getPlayer(it) } ?: return@put DoubleValue(
                        0.0
                    )
                DoubleValue(npc.distanceTo(serverPlayer))
            }
            map
        }
    }


    fun setupPlayerExtensions() {
        MoLangFunctions.playerFunctions.add { player ->
            val map = hashMapOf<String, java.util.function.Function<MoParams, Any>>()
            map.put("has_completed_task") { params ->
                val taskName = params.getString(0)
                return@put DoubleValue(if ((player get JourneyDataObject).completedQuests.contains(taskName.asResource())) 1.0 else 0.0)
            }
            map.put("has_completed_subtask") { params ->
                val taskName = params.getString(0).asResource()
                val subtaskName = params.getString(1)
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
                val count = params.getDoubleOrNull(1)?.toInt() ?: 1
                val hasItem = player.inventory.contains(
                    ItemStack(
                        BuiltInRegistries.ITEM.get(
                            ResourceLocation.parse(itemId)
                        )
                    )
                ) && player.inventory.countItem(
                    BuiltInRegistries.ITEM.get(
                        ResourceLocation.parse(itemId)
                    )
                ) >= count
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
            map.put("has_flag") { params ->
                val flag = params.getString(0)
                val data = player get JourneyDataObject
                DoubleValue(if (data.flags.contains(flag)) 1.0 else 0.0)
            }
            map.put("remove_item") { params ->
                val item = params.getString(0)
                val count = params.getDouble(1).toInt()
                player.inventory.items.first { it.item == BuiltInRegistries.ITEM.get(ResourceLocation.parse(item)) }
                    .shrink(count)
            }
            map.put("snowstorm_particle") { params ->
                val particle = params.getString(0).asResource()
                val x = params.getDouble(1)
                val y = params.getDouble(2)
                val z = params.getDouble(3)
                val pos = Vec3(x, y, z)
                val particlePacket = SpawnSnowstormParticlePacket(particle, pos)
                if (player == null) {
                    CobblemonNetwork.sendToAllPlayers(particlePacket)
                } else {
                    val serverPlayer = player as ServerPlayer
                    CobblemonNetwork.sendPacketToPlayer(serverPlayer, particlePacket)
                }
            }
            map.put("execute_command") { params ->
                val command = params.getString(0)
                player.server!!.commands.dispatcher.execute(command.replace("{player}", player.name.string), player.server!!.createCommandSourceStack())
            }
            map.put("launch_timeline") { params ->
                val timelineName = params.getString(0)
                val timeline = Timeline.TIMELINES[timelineName] ?: return@put DoubleValue(0.0)
                timeline.launch(player as ServerPlayer)
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

fun Pokemon.asStruct(): QueryStruct {
    return QueryStruct(
        hashMapOf(
            "species" to java.util.function.Function { params ->
                this.species.asStruct()
            },
            "level" to java.util.function.Function { params ->
                DoubleValue(this.level.toDouble())
            },
            "name" to java.util.function.Function { params ->
                StringValue(this.originalTrainerName)
            },
            "nickname" to java.util.function.Function { params ->
                StringValue(this.nickname.toString())
            },
            "is_shiny" to java.util.function.Function { params ->
                DoubleValue(if (this.shiny) 1.0 else 0.0)
            },
            "is_fainted" to java.util.function.Function { params ->
                DoubleValue(if (this.isFainted()) 1.0 else 0.0)
            },
            "form" to java.util.function.Function { params ->
                StringValue(this.form.name)
            },
            "has_aspect" to java.util.function.Function { params ->
                val aspect = params.getString(0)
                DoubleValue(if (this.aspects.contains(aspect)) 1.0 else 0.0)
            },
            "is_type" to java.util.function.Function { params ->
                val type = params.getString(0)
                DoubleValue(if (this.types.any { it.name == type }) 1.0 else 0.0)
            },
            "ability" to java.util.function.Function { params ->
                StringValue(this.ability.name)
            },
            "gender" to java.util.function.Function { params ->
                StringValue(this.gender.name)
            },
            "nature" to java.util.function.Function { params ->
                StringValue(this.nature.name.toString())
            },
            "friendship" to java.util.function.Function { params ->
                DoubleValue(this.friendship.toDouble())
            },
            "iv" to java.util.function.Function { params ->
                val stat = Stats.getStat(params.getString(0))
                DoubleValue(this.ivs[stat]?.toDouble() ?: 0.0)
            },
            "ev" to java.util.function.Function { params ->
                val stat = Stats.getStat(params.getString(0))
                DoubleValue(this.evs[stat]?.toDouble() ?: 0.0)
            },
            "tera_type" to java.util.function.Function { params ->
                StringValue(this.teraType.id.toString())
            }
        )
    )
}

fun Species.asStruct(): QueryStruct {
    return QueryStruct(
        hashMapOf(
            "name" to java.util.function.Function { params ->
                StringValue(this.name)
            },
            "has_label" to java.util.function.Function { params ->
                val label = params.getString(0)
                DoubleValue(if (this.labels.contains(label)) 1.0 else 0.0)
            }
        )
    )
}