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
import aster.amo.journey.utils.stimuli.SphereFilter
import com.bedrockk.molang.runtime.struct.QueryStruct
import xyz.nucleoid.stimuli.filter.EventFilter
import java.util.*

class ZoneSphere(
    val center: BlockPos,
    val radius: Double,
    dimension: String,
    entryMessage: ConfigTitle = ConfigTitle(),
    exitMessage: ConfigTitle = ConfigTitle(),
    functions: List<String> = emptyList()
): ZoneArea(dimension, entryMessage, exitMessage, functions) {
    override fun contains(pos: BlockPos): Boolean {
        val distanceSquared = pos.distSqr(center)
        return distanceSquared <= radius * radius
    }

    override fun onTick(server: MinecraftServer) {
        val level = server.allLevels.firstOrNull { level -> level.dimension().location().toString() == dimension } ?: return
        val players = level.players().filter { this.contains(it.blockPosition()) && it.isAlive }
        val newPlayers = players.filter { it.uuid !in playersInside }
        val leavingPlayers = playersInside.filter { !players.any { player -> player.uuid == it } }

        newPlayers.forEach { player ->
            onPlayerEnter(player)
            playersInside.add(player.uuid)
        }

        players.forEach { onPlayerInside(it) }
        leavingPlayers.forEach { player ->
            onPlayerLeave(level.getPlayerByUUID(player) as? ServerPlayer?: return)
            playersInside.remove(player)
        }
    }

    override fun drawOutline(level: ServerLevel, particle: ParticleOptions, particlesPerBlock: Double, color: Vector3f) {
        val dust = DustParticleOptions(color, 0.5f)
        val segments = (radius * particlesPerBlock * 4).toInt() // Adjust multiplier for density
        val angleIncrement = 2 * Math.PI / segments

        // Draw circles along each axis
        for (i in 0 until segments) {
            val angle1 = angleIncrement * i
            val angle2 = angleIncrement * (i + 1)

            // X-Y Plane
            val x1 = center.x + radius * Math.cos(angle1)
            val y1 = center.y + radius * Math.sin(angle1)
            val x2 = center.x + radius * Math.cos(angle2)
            val y2 = center.y + radius * Math.sin(angle2)
            drawParticleLine(level, dust, Vec3(x1, y1, center.z.toDouble()), Vec3(x2, y2, center.z.toDouble()), particlesPerBlock, color)

            // X-Z Plane
            val x3 = center.x + radius * Math.cos(angle1)
            val z3 = center.z + radius * Math.sin(angle1)
            val x4 = center.x + radius * Math.cos(angle2)
            val z4 = center.z + radius * Math.sin(angle2)
            drawParticleLine(level, dust, Vec3(x3, center.y.toDouble(), z3), Vec3(x4, center.y.toDouble(), z4), particlesPerBlock, color)

            // Y-Z Plane
            val y5 = center.y + radius * Math.cos(angle1)
            val z5 = center.z + radius * Math.sin(angle1)
            val y6 = center.y + radius * Math.cos(angle2)
            val z6 = center.z + radius * Math.sin(angle2)
            drawParticleLine(level, dust, Vec3(center.x.toDouble(), y5, z5), Vec3(center.x.toDouble(), y6, z6), particlesPerBlock, color)
        }
    }

    override fun getStimuliZone(): EventFilter {
        return SphereFilter(dimensionKey, center.toVec3d(), radius)
    }

    override fun toStruct(): QueryStruct {
        return QueryStruct(
            hashMapOf(
                "type" to java.util.function.Function { "sphere" },
                "center" to java.util.function.Function { center.toVec3d() },
                "radius" to java.util.function.Function { radius },
                "dimension" to java.util.function.Function { dimensionKey.location().toString() },
            )
        )
    }
}