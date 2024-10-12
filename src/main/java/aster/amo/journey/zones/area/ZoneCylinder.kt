package aster.amo.journey.zones.area

import com.cobblemon.mod.common.util.asResource
import com.cobblemon.mod.common.util.toVec3d
import com.google.gson.annotations.SerializedName
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.DustParticleOptions
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import org.joml.Vector3f
import aster.amo.journey.utils.ConfigTitle
import aster.amo.journey.utils.ParticleUtil.drawParticleLine
import aster.amo.journey.utils.stimuli.CylinderFilter
import com.bedrockk.molang.runtime.struct.QueryStruct
import xyz.nucleoid.stimuli.filter.EventFilter
import java.util.*

class ZoneCylinder(
    val center: BlockPos,
    val radius: Double,
    val height: Double,
    dimension: String,
    entryMessage: ConfigTitle = ConfigTitle(),
    exitMessage: ConfigTitle = ConfigTitle(),
    functions: List<String> = emptyList()
): ZoneArea(dimension, entryMessage, exitMessage, functions) {
    override fun contains(pos: BlockPos): Boolean {
        val dx = pos.x - center.x
        val dz = pos.z - center.z
        val distanceSquared = dx * dx + dz * dz
        return distanceSquared <= radius * radius && pos.y.toDouble() in (center.y.toDouble()..(center.y + height))
    }

    override fun onTick(server: MinecraftServer) {
        val level = server.allLevels.firstOrNull { level -> level.dimension().location().toString() == dimension } ?: return
        val players = level.players().filter { this.contains(it.blockPosition()) }
        val newPlayers = players.filter { it.uuid !in playersInside }
        val leavingPlayers = playersInside.filter { !players.any { player -> player.uuid == it } }

        newPlayers.forEach { player ->
            onPlayerEnter(player)
            playersInside.add(player.uuid)
        }

        players.forEach { onPlayerInside(it) }
        leavingPlayers.forEach { player ->
            onPlayerLeave(level.getPlayerByUUID(player)!! as ServerPlayer)
            playersInside.remove(player)
        }
    }

    override fun drawOutline(level: ServerLevel, particle: ParticleOptions, particlesPerBlock: Double, color: Vector3f) {
        val dust = DustParticleOptions(color, 0.5f)
        val segments = (radius * particlesPerBlock * 4).toInt()
        val angleIncrement = 2 * Math.PI / segments

        // Draw top and bottom circles
        for (i in 0 until segments) {
            val angle1 = angleIncrement * i
            val angle2 = angleIncrement * (i + 1)

            // Top Circle
            val x1Top = center.x + radius * Math.cos(angle1)
            val z1Top = center.z + radius * Math.sin(angle1)
            val x2Top = center.x + radius * Math.cos(angle2)
            val z2Top = center.z + radius * Math.sin(angle2)
            drawParticleLine(level, dust, Vec3(x1Top, center.y + height, z1Top), Vec3(x2Top, center.y + height, z2Top), particlesPerBlock, color)

            // Bottom Circle
            val x1Bottom = center.x + radius * Math.cos(angle1)
            val z1Bottom = center.z + radius * Math.sin(angle1)
            val x2Bottom = center.x + radius * Math.cos(angle2)
            val z2Bottom = center.z + radius * Math.sin(angle2)
            drawParticleLine(level, dust, Vec3(x1Bottom, center.y.toDouble(), z1Bottom), Vec3(x2Bottom, center.y.toDouble(), z2Bottom), particlesPerBlock, color)
        }

        // Draw vertical lines connecting the circles
        for (i in 0 until segments step 16) {
            val angle = angleIncrement * i
            val x = center.x + radius * Math.cos(angle)
            val z = center.z + radius * Math.sin(angle)
            drawParticleLine(level, dust, Vec3(x, center.y.toDouble(), z), Vec3(x, center.y + height, z), particlesPerBlock, color)
        }
    }

    override fun getStimuliZone(): EventFilter {
        return CylinderFilter(dimensionKey, center.toVec3d(), radius, height)
    }

    override fun toStruct(): QueryStruct {
        return QueryStruct(
            hashMapOf(
                "type" to java.util.function.Function { "cylinder" },
                "center" to java.util.function.Function { center.toVec3d() },
                "radius" to java.util.function.Function { radius },
                "height" to java.util.function.Function { height },
                "dimension" to java.util.function.Function { dimensionKey.location().toString() }
            )
        )
    }
}