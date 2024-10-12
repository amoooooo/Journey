package aster.amo.journey

import aster.amo.ceremony.math.splines.MatrixSpline
import aster.amo.ceremony.utils.extension.get
import aster.amo.journey.commands.BaseCommand
import aster.amo.journey.config.ConfigManager
import aster.amo.journey.data.JourneyDataObject
import aster.amo.journey.task.Subtask
import aster.amo.journey.task.Task
import aster.amo.journey.task.TaskRegistry
import aster.amo.journey.task.event.JourneyEvent
import aster.amo.journey.task.event.JourneyEvents
import aster.amo.journey.task.reward.RewardTypeAdapterFactory
import aster.amo.journey.utils.JourneyMolang
import aster.amo.journey.utils.adapter.JourneyEventDeserializer
import aster.amo.journey.utils.adapter.TaskTypeAdapter
import aster.amo.journey.utils.Utils
import aster.amo.journey.utils.adapter.SubtaskTypeAdapter
import aster.amo.journey.utils.adapter.ZoneAreaTypeAdapter
import aster.amo.journey.utils.pathfinding.PathfinderService
import aster.amo.journey.zones.ZoneManager
import aster.amo.journey.zones.area.ZoneArea
import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.api.molang.ExpressionLike
import com.cobblemon.mod.common.util.adapters.ExpressionLikeAdapter
import com.cobblemon.mod.common.util.asUUID
import com.cobblemon.mod.common.util.math.geometry.toRadians
import com.github.shynixn.mccoroutine.fabric.launch
import com.github.shynixn.mccoroutine.fabric.mcCoroutineConfiguration
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.loader.api.FabricLoader
import net.kyori.adventure.platform.fabric.FabricServerAudiences
import net.kyori.adventure.text.minimessage.MiniMessage
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.DustParticleOptions
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.item.Item
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.joml.Vector3f
import org.joml.Vector3i
import java.io.File
import java.util.concurrent.Executor

class Journey : ModInitializer {
    companion object {
        lateinit var INSTANCE: Journey

        var MOD_ID = "journey"
        var MOD_NAME = "Journey"

        val LOGGER: Logger = LogManager.getLogger(MOD_ID)
        val MINI_MESSAGE: MiniMessage = MiniMessage.miniMessage()

        @JvmStatic
        fun asResource(path: String): ResourceLocation {
            return ResourceLocation.fromNamespaceAndPath(MOD_ID, path)
        }
    }

    lateinit var configDir: File
    lateinit var pathfinderService: PathfinderService
    lateinit var adventure: FabricServerAudiences
    var server: MinecraftServer? = null
    var zoneManager: ZoneManager = ZoneManager()
    var gson: Gson = GsonBuilder()
        .registerTypeAdapterFactory(RewardTypeAdapterFactory())
        .registerTypeAdapter(JourneyEvent::class.java, JourneyEventDeserializer())
        .registerTypeAdapter(Task::class.java, TaskTypeAdapter())
        .registerTypeAdapter(ExpressionLike::class.java, ExpressionLikeAdapter)
        .registerTypeAdapter(ZoneArea::class.java, ZoneAreaTypeAdapter)
        .registerTypeHierarchyAdapter(Item::class.java, Utils.RegistrySerializer(BuiltInRegistries.ITEM))
        .registerTypeHierarchyAdapter(SoundEvent::class.java, Utils.RegistrySerializer(BuiltInRegistries.SOUND_EVENT))
        .registerTypeHierarchyAdapter(CompoundTag::class.java, Utils.CodecSerializer(CompoundTag.CODEC))
        .registerTypeAdapter(Subtask::class.java, SubtaskTypeAdapter())
        .disableHtmlEscaping().create()

    var gsonPretty: Gson = gson.newBuilder().setPrettyPrinting().create()
    override fun onInitialize() {
        INSTANCE = this

        this.configDir = File(FabricLoader.getInstance().configDirectory, MOD_ID)
        ConfigManager.load()
        registerEvents()
        JourneyMolang.setupPlayerExtensions()
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun registerEvents() {
        ServerLifecycleEvents.SERVER_STARTING.register(ServerLifecycleEvents.ServerStarting { server: MinecraftServer? ->
            this.adventure = FabricServerAudiences.of(
                server!!
            )
            this.server = server
            mcCoroutineConfiguration.minecraftExecutor = Executor { r -> server.submit(r) }
        })
        ServerLifecycleEvents.SERVER_STARTED.register { server ->
            reload()
        }
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            BaseCommand().register(
                dispatcher
            )
        }
        ServerTickEvents.START_SERVER_TICK.register { server ->
            zoneManager.onTick(server)
            server.playerList.players.forEach { player ->
                val data = player get JourneyDataObject
                data.rebuildSidebar()
                data.getTrackedTask()?.let { task ->
                    val subtask = task.currentSubtaskId?.let { TaskRegistry.getSubtask(task.taskId, it) } ?: return@let
                    if (subtask.event == JourneyEvents.ENTITY_INTERACT) {
                        if (!subtask.eventData.has("uuid")) return@let
                        val uuid = subtask.eventData.get("uuid").toString().replace("\"", "")
                        val entity = uuid.asUUID!!.let { (player.level() as ServerLevel).getEntity(it) } ?: return@let
                        if (entity.distanceTo(player) < 32.0) {
                            GlobalScope.launch(Dispatchers.Default) {
                                val spline = MatrixSpline.spiralSpline(0.5, 2.0, 3, 64)
                                for (i in 0..48) {
                                    val rot = -(((player.level().gameTime * 2500) % 360)).toRadians()
                                    val point = spline.interpolate(i.toDouble() / 48.0).yRot(rot).add(entity.position())
                                    player.connection.send(
                                        ClientboundLevelParticlesPacket(
                                            DustParticleOptions(Vector3f(0.0f, 1.0f, 0.0f), 0.5f),
                                            true,
                                            point.x,
                                            point.y,
                                            point.z,
                                            0.0f,
                                            0.0f,
                                            0.0f,
                                            0.0f,
                                            1
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                TaskRegistry.TASKS.entries.forEach { taskEntry ->
                    if(!data.completedQuests.contains(taskEntry.key)) {
                        val task = taskEntry.value
                        if(task.starterNPC.isNotEmpty()) {
                            val doesTrackedTaskContainEntity: Boolean = data.getTrackedTask()?.let { trackedTask ->
                                val taskId = trackedTask.taskId
                                val subtaskId = trackedTask.currentSubtaskId ?: return@let false
                                val subtask = TaskRegistry.getSubtask(taskId, subtaskId) ?: return@let false
                                val subtaskUuid = subtask.eventData["uuid"]?.asJsonPrimitive?.asString ?: return@let false
                                subtask.event == JourneyEvents.ENTITY_INTERACT &&
                                        subtaskUuid == task.starterNPC
                            } ?: false
                            if(task.canStart(player) && !doesTrackedTaskContainEntity) {
                                val npc = (player.level() as ServerLevel).getEntity(task.starterNPC.asUUID!!) ?: return@forEach
                                if(npc.distanceTo(player) < 32.0) {
                                    GlobalScope.launch(Dispatchers.Default) {
                                        val spline = MatrixSpline.spiralSpline(0.5, 0.5, 3, 64)
                                        for (i in 0..48) {
                                            val rot = -(((player.level().gameTime * 2500) % 360)).toRadians()
                                            val point = spline.interpolate(i.toDouble() / 48.0).yRot(rot).add(npc.position()).add(0.0,2.0,0.0)
                                            player.connection.send(
                                                ClientboundLevelParticlesPacket(
                                                    DustParticleOptions(Vector3f(1.0f, 1.0f, 0.0f), 0.5f),
                                                    true,
                                                    point.x,
                                                    point.y,
                                                    point.z,
                                                    0.0f,
                                                    0.0f,
                                                    0.0f,
                                                    0.0f,
                                                    1
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        CobblemonEvents.STARTER_CHOSEN.subscribe { event ->
            (event.player get JourneyDataObject).starterPokemon = event.pokemon.uuid
        }
        JourneyEvents.init()
        ServerLifecycleEvents.SERVER_STARTED.register { server ->
            pathfinderService = PathfinderService(server)
            zoneManager.init()
        }
        ServerLifecycleEvents.SERVER_STOPPING.register { server ->
            pathfinderService.shutdown()
        }
    }

    fun reload() {
        ConfigManager.load()
    }
}

fun BlockPos.toVector3i(): Vector3i {
    return Vector3i(x, y, z)
}

fun Vector3i.toBlockPos(): BlockPos {
    return BlockPos(x, y, z)
}