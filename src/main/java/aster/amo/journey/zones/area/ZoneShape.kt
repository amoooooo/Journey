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
import net.minecraft.world.phys.Vec2
import org.joml.Vector3f
import aster.amo.journey.utils.ConfigTitle
import aster.amo.journey.utils.ParticleUtil.drawParticleLine
import aster.amo.journey.utils.stimuli.AreaFilter
import com.bedrockk.molang.runtime.struct.QueryStruct
import xyz.nucleoid.stimuli.filter.EventFilter
import java.util.*

class ZoneShape(
    val vertices: List<BlockPos>,
    val height: Double,
    dimension: String,
    entryMessage: ConfigTitle = ConfigTitle(),
    exitMessage: ConfigTitle = ConfigTitle(),
    functions: List<String> = emptyList()
): ZoneArea(dimension, entryMessage, exitMessage, functions) {

    override fun contains(pos: BlockPos): Boolean {
        // 1. Project to X-Z plane
        val point = Vec2(pos.x.toFloat(), pos.z.toFloat())

        // 2. Check Y coordinate against height
        if (pos.y.toFloat() !in (vertices[0].y.toDouble()..(vertices[0].y + height))) {
            return false
        }

        // 3. Point-in-polygon check (using ray casting algorithm)
        var inside = false
        for (i in vertices.indices) {
            val j = (i + 1) % vertices.size
            val vert1 = Vec2(vertices[i].x.toFloat(), vertices[i].z.toFloat())
            val vert2 = Vec2(vertices[j].x.toFloat(), vertices[j].z.toFloat())

            if ((vert1.y <= point.y && vert2.y > point.y || vert2.y <= point.y && vert1.y > point.y) &&
                point.x < (vert2.x - vert1.x) * (point.y - vert1.y) / (vert2.y - vert1.y) + vert1.x
            ) {
                inside = !inside
            }
        }
        return inside
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
            (level.getPlayerByUUID(player) as ServerPlayer?)?.let { onPlayerLeave(it) }
            playersInside.remove(player)
        }
    }

    override fun drawOutline(level: ServerLevel, particle: ParticleOptions, particlesPerBlock: Double, color: Vector3f) {
        val dust = DustParticleOptions(color, 0.5f)
        // Draw lines between vertices on the base
        for (i in 0 until vertices.size - 1) {
            drawParticleLine(level, dust, vertices[i].toVec3d(), vertices[i + 1].toVec3d(), particlesPerBlock, color)
        }
        drawParticleLine(level, dust, vertices.last().toVec3d(), vertices.first().toVec3d(), particlesPerBlock, color) // Close the loop

        // Draw vertical lines connecting to the top face
        vertices.forEach { vertex ->
            drawParticleLine(level, dust, vertex.toVec3d(), vertex.above(height.toInt()).toVec3d(), particlesPerBlock, color)
        }

        // Draw lines between vertices on the top face (same as base)
        for (i in 0 until vertices.size - 1) {
            drawParticleLine(level, dust, vertices[i].above(height.toInt()).toVec3d(), vertices[i + 1].above(height.toInt()).toVec3d(), particlesPerBlock, color)
        }
        drawParticleLine(level, dust, vertices.last().above(height.toInt()).toVec3d(), vertices.first().above(height.toInt()).toVec3d(), particlesPerBlock, color)
    }

    override fun getStimuliZone(): EventFilter {
        return AreaFilter(dimensionKey, vertices.map { it.toVec3d() }, height)
    }

    override fun toStruct(): QueryStruct {
        return QueryStruct(
            hashMapOf(
                "type" to java.util.function.Function { "polygon" },
                "vertices" to java.util.function.Function { vertices.map { it.toVec3d() } },
                "height" to java.util.function.Function { height }
            )
        )
    }
}