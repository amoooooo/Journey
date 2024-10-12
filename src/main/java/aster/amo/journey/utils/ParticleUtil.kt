package aster.amo.journey.utils

import net.minecraft.core.particles.DustParticleOptions
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.phys.Vec3
import org.joml.Vector3f
import aster.amo.journey.data.JourneyDataObject
import aster.amo.ceremony.utils.extension.get

object ParticleUtil {
    fun drawParticleLine(level: ServerLevel, particle: ParticleOptions, start: Vec3, end: Vec3, particlesPerBlock: Double, color: Vector3f) {
        val dust = DustParticleOptions(color, 1f) // Create DustParticleOptions here
        val lineLength = start.distanceTo(end)
        val particleCount = (lineLength * particlesPerBlock).toInt()
        val delta = end.subtract(start).scale(1.0 / particleCount.toDouble())
        for (i in 0 until particleCount) {
            val pos = start.add(delta.scale(i.toDouble()))
            level.players().forEach { player ->
                if(player.distanceToSqr(pos.x, pos.y, pos.z) < 32.0 * 32.0 && player[JourneyDataObject].showZoneBounds) {
                    player.connection.send(
                        ClientboundLevelParticlesPacket(
                            dust,
                            true,
                            pos.x,
                            pos.y,
                            pos.z,
                            0.25f,
                            0.25f,
                            0.25f,
                            0.0f,
                            1
                        )
                    )
                }
            }
        }
    }
}