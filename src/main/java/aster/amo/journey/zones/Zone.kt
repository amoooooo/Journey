package aster.amo.journey.zones

import com.bedrockk.molang.runtime.struct.QueryStruct
import com.google.gson.annotations.SerializedName
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.DustParticleOptions
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import aster.amo.journey.utils.Color
import aster.amo.journey.utils.ConfigTitle
import aster.amo.journey.zones.area.ZoneArea
import aster.amo.journey.event.EnterZoneEvent
import aster.amo.journey.event.ExitZoneEvent
import aster.amo.journey.event.InsideZoneEvent
import com.bedrockk.molang.runtime.value.StringValue
import xyz.nucleoid.stimuli.Stimuli
import java.util.*

class Zone(
    @SerializedName("name") val name: String? = null,
    @SerializedName("uuid") val uuid: UUID = UUID.randomUUID(),
    @SerializedName("areas") val areas: MutableList<ZoneArea> = mutableListOf(),
    @SerializedName("color") val color: Color = Color.random(),
    @SerializedName("entryMessage") val entryMessage: ConfigTitle = ConfigTitle(),
    @SerializedName("exitMessage") val exitMessage: ConfigTitle = ConfigTitle()
) {
    @Transient
    val playersInside = mutableListOf<UUID>()

    fun addArea(area: ZoneArea) {
        areas.add(area)
    }

    fun removeArea(area: ZoneArea) {
        areas.remove(area)
    }

    fun findAreaContaining(pos: BlockPos): ZoneArea? {
        return areas.firstOrNull { it.contains(pos) }
    }

    fun contains(pos: BlockPos): Boolean {
        return areas.any { it.contains(pos) }
    }

    fun dimension(): String {
        return areas.firstOrNull()?.dimension ?: ""
    }

    fun onTick(server: MinecraftServer) {
        areas.forEach {
            it.onTick(server)
            server.allLevels.firstOrNull { level -> level.dimension().location().toString() == it.dimension }
                ?.let { it1 ->
                    it.drawOutline(
                        it1,
                        DustParticleOptions(color.toVector3f(), 0.5f),
                        1.0,
                        color.toVector3f()
                    )
                }
        }
        val level = server.allLevels.firstOrNull { level -> level.dimension().location().toString() == dimension() } ?: return
        val players = level.players().filter { this.contains(it.blockPosition()) }
        val newPlayers = players.filter { it.uuid !in playersInside }
        val leavingPlayers = playersInside.filter { !players.any { player -> player.uuid == it } }

        newPlayers.forEach { player ->
            onPlayerEnter(player)
            playersInside.add(player.uuid)
        }

        players.forEach { onPlayerInside(it) }
        leavingPlayers.forEach { player ->
            (level.getPlayerByUUID(player) as ServerPlayer?)?.let { onPlayerLeave(it) }
            playersInside.remove(player)
        }
    }

    fun onPlayerEnter(player: ServerPlayer) {
        if(entryMessage != null) {
            entryMessage.type.run(player, entryMessage)
        }
        Stimuli.select().forEntity(player).get(EnterZoneEvent.EVENT).onEnterZone(this, player)
    }

    fun onPlayerLeave(player: ServerPlayer) {
        if(exitMessage != null) {
            exitMessage.type.run(player, exitMessage)
        }
        Stimuli.select().forEntity(player).get(ExitZoneEvent.EVENT).onExitZone(this, player)
    }

    fun onPlayerInside(player: ServerPlayer) {
        Stimuli.select().forEntity(player).get(InsideZoneEvent.EVENT).onInsideZone(this, player)
    }

    fun toStruct(): QueryStruct {
        return QueryStruct(
            hashMapOf(
                "name" to java.util.function.Function { StringValue(name) },
                "uuid" to java.util.function.Function { StringValue(uuid.toString()) },
                "areas" to java.util.function.Function {
                    areas.map { area ->
                        val value = QueryStruct(
                            hashMapOf(
                                "dimension" to java.util.function.Function { area.dimension },
                                "entry_message" to java.util.function.Function { area.entryMessage },
                                "exit_message" to java.util.function.Function { area.exitMessage },
                                "tags" to java.util.function.Function { area.tags }
                            )
                        )
                        return@map value
                    }
                },
                "color" to java.util.function.Function { color.hex }
            )
        )
    }
}
