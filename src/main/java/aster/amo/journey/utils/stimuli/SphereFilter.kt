package aster.amo.journey.utils.stimuli

import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import xyz.nucleoid.stimuli.EventSource
import xyz.nucleoid.stimuli.filter.EventFilter

data class SphereFilter(
    val dimension: ResourceKey<Level>,
    val center: Vec3,
    val radius: Double
) : EventFilter {
    override fun accepts(source: EventSource): Boolean {
        val pos = source.pos ?: return false
        val resourceKey: ResourceKey<Level> = source.dimension ?: return false
        return resourceKey == dimension && contains(pos)
    }

    private fun contains(pos: BlockPos): Boolean {
        val dx = pos.x - center.x
        val dy = pos.y - center.y
        val dz = pos.z - center.z
        return dx * dx + dy * dy + dz * dz <= radius * radius
    }

}
