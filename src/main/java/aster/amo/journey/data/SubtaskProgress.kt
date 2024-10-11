package aster.amo.journey.data

import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation

class SubtaskProgress(
    val taskId: ResourceLocation,
    val subtaskId: String,
    var progress: Double = 0.0
) {
    fun toNbt(): CompoundTag {
        val tag = CompoundTag()
        tag.putString("taskId", taskId.toString())
        tag.putString("subtaskId", subtaskId)
        tag.putDouble("progress", progress)
        return tag
    }

    companion object {
        fun fromNbt(tag: CompoundTag): SubtaskProgress {
            val taskId = ResourceLocation.parse(tag.getString("taskId"))
            val subtaskId = tag.getString("subtaskId")
            val progress = tag.getDouble("progress")
            return SubtaskProgress(taskId, subtaskId, progress)
        }
    }
}
