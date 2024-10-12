package aster.amo.journey.utils.stimuli

import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import xyz.nucleoid.stimuli.EventSource
import xyz.nucleoid.stimuli.filter.EventFilter

data class CylinderFilter(
    val dimension: ResourceKey<Level>,
    val center: Vec3,
    val radius: Double,
    val height: Double
) : EventFilter {
    override fun accepts(source: EventSource): Boolean {
        val pos = source.pos ?: return false
        val resourceKey: ResourceKey<Level> = source.dimension ?: return false
        return resourceKey == dimension && contains(pos)
    }

    private fun contains(pos: BlockPos): Boolean {
        val dx = pos.x - center.x
        val dz = pos.z - center.z
        val distanceSquared = dx * dx + dz * dz
        return distanceSquared <= radius * radius && pos.y.toDouble() in (center.y.toDouble()..(center.y + height))
    }

}
