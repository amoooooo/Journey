package aster.amo.journey.flag

import com.cobblemon.mod.common.util.asResource
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType

class FilteredEntity(
    val uuid: String = "",
    val entityType: String = "",
    val filter: String = ""
) {
    companion object {
        val filters = mutableListOf<FilteredEntity>()
    }
    val realEntityType: EntityType<*> by lazy {
        BuiltInRegistries.ENTITY_TYPE.get(entityType.asResource()) ?: EntityType.PIG
    }

    fun matches(entityType: EntityType<*>) = entityType == realEntityType

    fun matches(uuid: String) = this.uuid == uuid

    fun matches(entityType: EntityType<*>, uuid: String) = matches(entityType) && matches(uuid)

    fun matches(entity: Entity) = matches(entity.type) || matches(entity.uuid.toString())
}