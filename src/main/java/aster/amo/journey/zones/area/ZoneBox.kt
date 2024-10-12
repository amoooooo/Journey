package aster.amo.journey.zones.area

import com.cobblemon.mod.common.util.asResource
import com.cobblemon.mod.common.util.toBlockPos
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
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Vector3f
import aster.amo.journey.utils.ConfigTitle
import aster.amo.journey.utils.ParticleUtil.drawParticleLine
import com.bedrockk.molang.runtime.struct.QueryStruct
import com.bedrockk.molang.runtime.value.StringValue
import xyz.nucleoid.stimuli.filter.EventFilter
import java.util.*

class ZoneBox(
    @SerializedName("corner1") var corner1: Vec3,
    @SerializedName("corner2") var corner2: Vec3,
    dimension: String,
    entryMessage: ConfigTitle = ConfigTitle(),
    exitMessage: ConfigTitle = ConfigTitle(),
    functions: List<String> = emptyList()
): ZoneArea(dimension, entryMessage, exitMessage, functions) {
    @Transient
    var initialized = false

    var aabb: AABB = AABB(
        corner1.x, corner1.y, corner1.z,
        corner2.x, corner2.y, corner2.z
    ).inflate(0.5) // Inflate slightly to catch players on the edge


    override fun contains(pos: BlockPos): Boolean {
        return aabb.contains(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())
    }

    override fun onTick(server: MinecraftServer) {
        if (!initialized) {
            dimensionKey = ResourceKey.create(Registries.DIMENSION, dimension.asResource())
            aabb = AABB(
                corner1.x, corner1.y, corner1.z,
                corner2.x, corner2.y, corner2.z
            ).inflate(0.5)
            playersInside.clear()
            initialized = true
        }
        val level = server.allLevels.firstOrNull { level -> level.dimension().location().toString() == dimension } ?: return
        val players = level.players().filter { this.contains(it.blockPosition()) }
        val newPlayers = players.filter { it.uuid !in playersInside }
        val leavingPlayers = playersInside.filter { !players.any { player -> player.uuid == it } }

        newPlayers.forEach { player ->
            onPlayerEnter(player)
            playersInside.add(player.uuid)
        }

        players.forEach { onPlayerInside(it) }

        leavingPlayers.forEach { uuid ->
            playersInside.remove(uuid)
            level.getPlayerByUUID(uuid)?.let { onPlayerLeave(it as ServerPlayer) }
        }


    }

    override fun drawOutline(level: ServerLevel, particle: ParticleOptions, particlesPerBlock: Double, color: Vector3f) {
        val dust = DustParticleOptions(color, 0.5f)

        // Bottom Face
        drawParticleLine(level, dust, Vec3(aabb.minX, aabb.minY, aabb.minZ), Vec3(aabb.maxX, aabb.minY, aabb.minZ), particlesPerBlock, color)
        drawParticleLine(level, dust, Vec3(aabb.maxX, aabb.minY, aabb.minZ), Vec3(aabb.maxX, aabb.minY, aabb.maxZ), particlesPerBlock, color)
        drawParticleLine(level, dust, Vec3(aabb.maxX, aabb.minY, aabb.maxZ), Vec3(aabb.minX, aabb.minY, aabb.maxZ), particlesPerBlock, color)
        drawParticleLine(level, dust, Vec3(aabb.minX, aabb.minY, aabb.maxZ), Vec3(aabb.minX, aabb.minY, aabb.minZ), particlesPerBlock, color)

        // Vertical Edges
        drawParticleLine(level, dust, Vec3(aabb.minX, aabb.minY, aabb.minZ), Vec3(aabb.minX, aabb.maxY, aabb.minZ), particlesPerBlock, color)
        drawParticleLine(level, dust, Vec3(aabb.maxX, aabb.minY, aabb.minZ), Vec3(aabb.maxX, aabb.maxY, aabb.minZ), particlesPerBlock, color)
        drawParticleLine(level, dust, Vec3(aabb.maxX, aabb.minY, aabb.maxZ), Vec3(aabb.maxX, aabb.maxY, aabb.maxZ), particlesPerBlock, color)
        drawParticleLine(level, dust, Vec3(aabb.minX, aabb.minY, aabb.maxZ), Vec3(aabb.minX, aabb.maxY, aabb.maxZ), particlesPerBlock, color)

        // Top Face
        drawParticleLine(level, dust, Vec3(aabb.minX, aabb.maxY, aabb.minZ), Vec3(aabb.maxX, aabb.maxY, aabb.minZ), particlesPerBlock, color)
        drawParticleLine(level, dust, Vec3(aabb.maxX, aabb.maxY, aabb.minZ), Vec3(aabb.maxX, aabb.maxY, aabb.maxZ), particlesPerBlock, color)
        drawParticleLine(level, dust, Vec3(aabb.maxX, aabb.maxY, aabb.maxZ), Vec3(aabb.minX, aabb.maxY, aabb.maxZ), particlesPerBlock, color)
        drawParticleLine(level, dust, Vec3(aabb.minX, aabb.maxY, aabb.maxZ), Vec3(aabb.minX, aabb.maxY, aabb.minZ), particlesPerBlock, color)
    }

    override fun getStimuliZone(): EventFilter {
        return EventFilter.box(dimensionKey, corner1.toBlockPos(), corner2.toBlockPos())
    }

    override fun toStruct(): QueryStruct {
        return QueryStruct(
            hashMapOf(
                "type" to java.util.function.Function {
                    return@Function StringValue("box")
                },
                "corner1" to java.util.function.Function {
                    return@Function QueryStruct(
                        hashMapOf(
                            "x" to java.util.function.Function { corner1.x },
                            "y" to java.util.function.Function { corner1.y },
                            "z" to java.util.function.Function { corner1.z }
                        )
                    )
                },
                "corner2" to java.util.function.Function {
                    return@Function QueryStruct(
                        hashMapOf(
                            "x" to java.util.function.Function { corner2.x },
                            "y" to java.util.function.Function { corner2.y },
                            "z" to java.util.function.Function { corner2.z }
                        )
                    )
                },
                "dimension" to java.util.function.Function { dimension }
            )
        )
    }
}
