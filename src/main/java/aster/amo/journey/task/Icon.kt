package aster.amo.journey.task

import com.cobblemon.mod.common.util.asResource
import com.google.gson.annotations.SerializedName
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.ItemStack
import aster.amo.journey.utils.parseToNative
import aster.amo.journey.utils.setLore
import aster.amo.journey.utils.withModelData
import aster.amo.journey.utils.withName

data class Icon(
    @SerializedName("item_id") val itemID: String = "",
    @SerializedName("item_lore") val itemLore: List<String> = listOf(),
    @SerializedName("item_display_name") val itemDisplayName: String = "",
    @SerializedName("custom_model_data") val customModelData: Int = 0
) {
    fun asStack(): ItemStack {
        val stack = BuiltInRegistries.ITEM.get(itemID.asResource())
        val itemStack = stack.defaultInstance
            .withName(itemDisplayName.parseToNative())
            .withModelData(customModelData)
            .setLore(itemLore.map { it.parseToNative() })
        return itemStack
    }
}