package aster.amo.journey.utils.stimuli

import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import xyz.nucleoid.stimuli.EventSource
import xyz.nucleoid.stimuli.filter.EventFilter

data class AreaFilter(
    val dimension: ResourceKey<Level>,
    val points: List<Vec3>,
    val height: Double
) : EventFilter {
    override fun accepts(source: EventSource): Boolean {
        val pos = source.pos ?: return false
        val resourceKey: ResourceKey<Level> = source.dimension ?: return false
        return resourceKey == dimension && contains(pos) && pos.y <= height + (points.maxOf { it.y })
    }

    // point-in-polygon algorithm
    private fun contains(pos: BlockPos): Boolean {
        val x = pos.x.toDouble()
        val z = pos.z.toDouble()
        var inside = false
        for (i in points.indices) {
            val xi = points[i].x
            val zi = points[i].z
            val xj = points[(i + 1) % points.size].x
            val zj = points[(i + 1) % points.size].z
            val intersect = zi > z != zj > z && x < (xj - xi) * (z - zi) / (zj - zi) + xi
            if (intersect) inside = !inside
        }
        return inside
    }

}
