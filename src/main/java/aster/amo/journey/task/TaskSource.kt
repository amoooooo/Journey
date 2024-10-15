package aster.amo.journey.task

import aster.amo.ceremony.math.splines.MatrixSpline
import aster.amo.ceremony.utils.extension.get
import aster.amo.journey.data.JourneyDataObject
import aster.amo.journey.task.event.createMolangRuntime
import aster.amo.journey.utils.MolangUtils
import com.bedrockk.molang.runtime.struct.QueryStruct
import com.cobblemon.mod.common.CobblemonNetwork
import com.cobblemon.mod.common.api.scripting.CobblemonScripts
import com.cobblemon.mod.common.entity.npc.NPCEntity
import com.cobblemon.mod.common.net.messages.client.effect.SpawnSnowstormEntityParticlePacket
import com.cobblemon.mod.common.net.messages.client.effect.SpawnSnowstormParticlePacket
import com.cobblemon.mod.common.util.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.core.particles.DustParticleOptions
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.Vec3
import org.joml.Vector3f
import org.spongepowered.include.com.google.gson.annotations.SerializedName
import java.util.UUID

class TaskSource(
    val uuid: String = "",
    val tasks: MutableMap<String, SourceInfo> = mutableMapOf(),
    val script: String = "",
    @SerializedName("marker_position") var markerPosition: Vector3f = Vector3f(0.0f, 2.0f, 0.0f),
) {
    data class SourceInfo(
        val condition: String =  "",
        val source: Boolean = false
    )
    @Transient val nearbyPlayers = mutableSetOf<UUID>()

    companion object {
        private val taskSources = mutableMapOf<String, TaskSource>()

        fun getTaskSource(uuid: String): TaskSource {
            return taskSources.getOrPut(uuid) { TaskSource(uuid, mutableMapOf()) }
        }

        fun removeTaskSource(uuid: String) {
            taskSources.remove(uuid)
        }

        fun setup() {
            ServerTickEvents.START_SERVER_TICK.register { server ->
                server.allLevels.forEach { level ->
                    taskSources.forEach { (name, taskSource) ->
                        taskSource.tasks.forEach { id, query ->
                            val entity = taskSource.uuid.asUUID?.let { level.getEntity(it) } ?: return@forEach
                            val currentNearbyPlayers = level.players().filter { it.distanceTo(entity) < 64.0 }
                            currentNearbyPlayers.forEach { player ->
                                val data = player get JourneyDataObject
                                val runtime = createMolangRuntime()
                                val queryStruct = runtime.environment.getStruct("query") as QueryStruct
                                MolangUtils.setupPlayerStructs(queryStruct, player)
                                if(entity is NPCEntity) {
                                    MolangUtils.setupNPCStructs(queryStruct, entity)
                                } else if (entity is LivingEntity) {
                                    MolangUtils.setupEntityStructs(queryStruct, entity)
                                }
                                val result = runtime.resolveBoolean(query.condition.asExpressionLike())
                                CobblemonScripts.run(taskSource.script.asResource(), runtime)
                                if(result){
                                    TaskStateEvaluator()
                                        .onAvailable {
                                            if(entity is NPCEntity) {
                                                CobblemonNetwork.sendPacketToPlayer(player, SpawnSnowstormEntityParticlePacket("cobblemon:quest_ready_exclamation".asResource(), entity.id, listOf("head")))
                                            } else {
                                            }
                                            CobblemonNetwork.sendPacketToPlayer(player, SpawnSnowstormParticlePacket("cobblemon:quest_ready_exclamation".asResource(), entity.position().add(taskSource.markerPosition.toVec3d())))

                                        }
                                        .onSubtaskTarget {
                                            if(entity is NPCEntity) {
                                                CobblemonNetwork.sendPacketToPlayer(player, SpawnSnowstormEntityParticlePacket("cobblemon:dialogue_exclamation".asResource(), entity.id, listOf("head")))
                                            } else {
                                            }
                                            CobblemonNetwork.sendPacketToPlayer(player, SpawnSnowstormParticlePacket("cobblemon:dialogue_exclamation".asResource(), entity.position().add(taskSource.markerPosition.toVec3d())))

                                        }
                                        .toHandIn {
                                            if(entity is NPCEntity) {
                                                CobblemonNetwork.sendPacketToPlayer(player, SpawnSnowstormEntityParticlePacket("cobblemon:quest_completed_question".asResource(), entity.id, listOf("head")))
                                            } else {
                                            }
                                            CobblemonNetwork.sendPacketToPlayer(player, SpawnSnowstormParticlePacket("cobblemon:quest_completed_question".asResource(), entity.position().add(taskSource.markerPosition.toVec3d())))

                                        }.evaluate(id.asResource(), entity.uuid, data, taskSource)
                                }
                            }
                        }

                    }
                }
            }
        }

        fun sendExclamationMarkParticles(player: ServerPlayer, position: Vec3) {
            val yellowColor = Vector3f(1.0f, 1.0f, 0.0f)
            val dustSize = 1.0f
            val dustParticle = DustParticleOptions(yellowColor, dustSize)
            val particleOffsets = listOf(
                Vec3(0.0, 0.0, 0.0),
                Vec3(0.0, 0.3, 0.0),
                Vec3(0.0, 0.6, 0.0),
                Vec3(0.0, -0.3, 0.0)
            )
            for (offset in particleOffsets) {
                val particlePosition = position.add(offset)
                player.connection.send(ClientboundLevelParticlesPacket(dustParticle, true, particlePosition.x, particlePosition.y, particlePosition.z, 0.0f, 0.0f, 0.0f, 0.0f, 1))
            }
        }

        fun sendQuestionMarkParticles(player: ServerPlayer, position: Vec3) {
            val yellowColor = Vector3f(1.0f, 1.0f, 0.0f)
            val dustSize = 1.0f
            val dustParticle = DustParticleOptions(yellowColor, dustSize)

            val particleOffsets = listOf(
                Vec3(0.0, 0.6, 0.0),
                Vec3(0.1, 0.5, 0.0),
                Vec3(0.2, 0.4, 0.0),
                Vec3(0.1, 0.3, 0.0),
                Vec3(0.0, 0.2, 0.0),
                Vec3(-0.1, 0.1, 0.0),
                Vec3(-0.2, 0.0, 0.0),
                Vec3(0.0, -0.3, 0.0)
            )

            for (offset in particleOffsets) {
                val particlePosition = position.add(offset)

                player.connection.send(ClientboundLevelParticlesPacket(dustParticle, true, particlePosition.x, particlePosition.y, particlePosition.z, 0.0f, 0.0f, 0.0f, 0.0f, 1))
            }
        }

        fun sendCheckMarkParticles(player: ServerPlayer, position: Vec3) {
            val greenColor = Vector3f(0.0f, 1.0f, 0.0f)
            val dustSize = 1.0f
            val dustParticle = DustParticleOptions(greenColor, dustSize)

            val particleOffsets = listOf(
                Vec3(-0.3, 0.0, 0.0),
                Vec3(-0.2, 0.1, 0.0),
                Vec3(-0.1, 0.2, 0.0),
                Vec3(0.0, 0.3, 0.0),

                Vec3(0.1, 0.2, 0.0),
                Vec3(0.2, 0.1, 0.0),
                Vec3(0.3, 0.0, 0.0)
            )

            for (offset in particleOffsets) {
                val particlePosition = position.add(offset)

                player.connection.send(ClientboundLevelParticlesPacket(dustParticle, true, particlePosition.x, particlePosition.y, particlePosition.z, 0.0f, 0.0f, 0.0f, 0.0f, 1))
            }
        }

        fun registerSource(rl: ResourceLocation, source: TaskSource) {
            taskSources[rl.toString()] = source
        }

        fun getSourceIDs(): Set<String> {
            return taskSources.keys
        }

        fun clear() {
            taskSources.clear()
        }
    }

}