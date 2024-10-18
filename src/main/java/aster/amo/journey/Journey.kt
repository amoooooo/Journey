package aster.amo.journey

import aster.amo.ceremony.math.splines.MatrixSpline
import aster.amo.ceremony.utils.extension.get
import aster.amo.journey.commands.BaseCommand
import aster.amo.journey.config.ConfigManager
import aster.amo.journey.data.JourneyDataObject
import aster.amo.journey.flag.PerPlayerStructure
import aster.amo.journey.task.*
import aster.amo.journey.task.event.JourneyEvent
import aster.amo.journey.task.event.JourneyEvents
import aster.amo.journey.task.reward.RewardTypeAdapterFactory
import aster.amo.journey.timeline.Timeline
import aster.amo.journey.timeline.Timeline.*
import aster.amo.journey.timeline.Timeline.ActionTypeAdapter
import aster.amo.journey.utils.JourneyMolang
import aster.amo.journey.utils.MolangUtils
import aster.amo.journey.utils.StructureThread
import aster.amo.journey.utils.Utils
import aster.amo.journey.utils.adapter.*
import aster.amo.journey.utils.pathfinding.PathfinderService
import aster.amo.journey.zones.ZoneManager
import aster.amo.journey.zones.area.ZoneArea
import com.bedrockk.molang.MoLang
import com.bedrockk.molang.runtime.MoLangEnvironment
import com.bedrockk.molang.runtime.MoLangRuntime
import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.api.molang.ExpressionLike
import com.cobblemon.mod.common.api.molang.MoLangFunctions
import com.cobblemon.mod.common.api.molang.MoLangFunctions.setup
import com.cobblemon.mod.common.api.scripting.CobblemonScripts
import com.cobblemon.mod.common.util.adapters.ExpressionLikeAdapter
import com.cobblemon.mod.common.util.asResource
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
        .registerTypeAdapter(Vector3f::class.java, Vector3fTypeAdapter())
        .registerTypeAdapter(TaskSource::class.java, TaskSourceTypeAdapter(Vector3fTypeAdapter()))
        .registerTypeAdapter(Action::class.java, ActionTypeAdapter())
        .registerTypeAdapter(ActionsAtTick::class.java, ActionsAtTickTypeAdapter())
        .registerTypeAdapter(Timeline::class.java, TimelineTypeAdapter())
        .disableHtmlEscaping().create()

    var gsonPretty: Gson = gson.newBuilder().setPrettyPrinting().create()

    val structureThread: StructureThread = StructureThread()
    override fun onInitialize() {
        INSTANCE = this
        structureThread.launch()
        this.configDir = File(FabricLoader.getInstance().configDirectory, MOD_ID)
        ConfigManager.load()
        registerEvents()
        JourneyMolang.setupPlayerExtensions()
        JourneyMolang.setupNPCExtensions()
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun registerEvents() {
        TaskSource.setup()
        RepeatHandler.setupReset()
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
            PerPlayerStructure.send(server)
            server.playerList.players.forEach { player ->
                val data = player get JourneyDataObject
                data.tasksProgress.keys.forEach { taskID ->
                    val task = Task.TASKS[taskID] ?: return@forEach
                    val taskScript = task.script
                    if (taskScript.isNotEmpty()) {
                        GlobalScope.launch(Dispatchers.IO) {
                            val runtime = MoLangRuntime().setup()
                            MolangUtils.setupPlayerStructs(runtime.environment.query, player)
                            MolangUtils.setupWorldStructs(runtime.environment.query, player)
                            CobblemonScripts.run(taskScript.asResource(), runtime)
                        }
                    }
                }
                data.rebuildSidebar()
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