package aster.amo.journey

import aster.amo.ceremony.math.splines.MatrixSpline
import aster.amo.ceremony.utils.extension.get
import aster.amo.journey.commands.BaseCommand
import aster.amo.journey.config.ConfigManager
import aster.amo.journey.data.JourneyDataObject
import aster.amo.journey.task.Task
import aster.amo.journey.task.TaskRegistry
import aster.amo.journey.task.event.JourneyEvent
import aster.amo.journey.task.event.JourneyEvents
import aster.amo.journey.task.reward.RewardTypeAdapterFactory
import aster.amo.journey.utils.JourneyEventDeserializer
import aster.amo.journey.utils.TaskTypeAdapter
import aster.amo.journey.utils.pathfinding.PathfinderService
import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.github.shynixn.mccoroutine.fabric.mcCoroutineConfiguration
import com.github.shynixn.mccoroutine.fabric.minecraftDispatcher
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.runBlocking
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.loader.api.FabricLoader
import net.kyori.adventure.platform.fabric.FabricServerAudiences
import net.kyori.adventure.text.minimessage.MiniMessage
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.DustParticleOptions
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.world.phys.Vec3
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.joml.Vector3f
import org.joml.Vector3i
import java.io.File
import java.util.concurrent.Executor
import kotlin.coroutines.coroutineContext

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

    var gson: Gson = GsonBuilder()
        .registerTypeAdapterFactory(RewardTypeAdapterFactory())
        .registerTypeAdapter(JourneyEvent::class.java, JourneyEventDeserializer())
        .registerTypeAdapter(Task::class.java, TaskTypeAdapter())
        .disableHtmlEscaping().create()

    var gsonPretty: Gson = gson.newBuilder().setPrettyPrinting().create()
    override fun onInitialize() {
        INSTANCE = this

        this.configDir = File(FabricLoader.getInstance().configDirectory, MOD_ID)
        ConfigManager.load()
        registerEvents()
    }

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
            server.playerList.players.forEach { player ->
                val data = player get JourneyDataObject
                data.rebuildSidebar()
//                data.getTrackedTask()?.let { task ->
//                    val subtask = task.currentSubtaskId?.let { TaskRegistry.getSubtask(task.taskId, it) } ?: return@let
//                    if (subtask.location != null) {
//                        runBlocking {
//
//                        }
//                        val start = player.blockPosition().above().toVector3i()
//                        val end = subtask.location
//                        val parrot = (player get JourneyDataObject).pathfindingDummy
//                        parrot.setPos(start.x.toDouble(), start.y.toDouble(), start.z.toDouble())
//                        parrot.navigation.moveTo(end.x.toDouble(), end.y.toDouble() + 1, end.z.toDouble(), 5.0)
//                        parrot.navigation.path.also { it ->
//                            val nodeCount = it?.nodeCount ?: return@also
//                            val path = mutableListOf<Vector3i>()
//                            for (i in 0 until nodeCount) {
//                                val node = it.getNode(i)
//                                path.add(Vector3i(node.x, node.y, node.z))
//                            }
//                            // loop over the path and add 10 points between each point to make it smoother
//                            if (path.size >= 4) {
//                                runBlocking(minecraftDispatcher) {
//                                    val spline = MatrixSpline.bezierSpline(*path.map { p ->
//                                        Vec3(
//                                            p.x.toDouble(),
//                                            p.y.toDouble(),
//                                            p.z.toDouble()
//                                        )
//                                    }.toTypedArray())
//                                    for (i in 0 until path.size * 20) {
//                                        val pos = spline.interpolate(i / (path.size * 20.0))
//                                        player.connection.send(
//                                            ClientboundLevelParticlesPacket(
//                                                DustParticleOptions(Vector3f(0.0f, 1.0f, 0.85f), 0.2f),
//                                                false,
//                                                pos.x,
//                                                pos.y,
//                                                pos.z,
//                                                0.0f,
//                                                0.0f,
//                                                0.0f,
//                                                0.0f,
//                                                1
//                                            )
//                                        )
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
            }
        }
        CobblemonEvents.STARTER_CHOSEN.subscribe { event ->
            (event.player get JourneyDataObject).starterPokemon = event.pokemon.uuid
        }
        JourneyEvents.init()


// Register the PathfinderService on server start
        ServerLifecycleEvents.SERVER_STARTED.register { server ->
            pathfinderService = PathfinderService(server)

        }

// Shutdown the PathfinderService gracefully
        ServerLifecycleEvents.SERVER_STOPPING.register { server ->
            // Assuming you have a reference to the PathfinderService
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