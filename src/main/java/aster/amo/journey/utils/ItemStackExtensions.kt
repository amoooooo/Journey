package aster.amo.journey.utils

import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomModelData
import net.minecraft.world.item.component.ItemLore

fun ItemStack.setLore(lore: List<Component>): ItemStack {
    if (lore.isNotEmpty()) {
        this.set(DataComponents.LORE, ItemLore(lore))
    }
    return this
}

fun ItemStack.withModelDataCopy(data: Int): ItemStack {
    val stack = this.copy()
    stack.set(DataComponents.CUSTOM_MODEL_DATA, CustomModelData(data))
    return stack
}

fun ItemStack.withModelData(data: Int): ItemStack {
    this.set(DataComponents.CUSTOM_MODEL_DATA, CustomModelData(data))
    return this
}

fun ItemStack.withName(name: Component): ItemStack {
    this.set(DataComponents.ITEM_NAME, Component.empty().setStyle(Style.EMPTY.withItalic(false)).append(name.copy().withStyle { style -> style.withItalic(false) }))
    return this
}

fun ItemStack.withNameCopy(name: Component): ItemStack {
    this.set(DataComponents.ITEM_NAME, Component.empty().setStyle(Style.EMPTY.withItalic(false)).append(name.copy().withStyle { style -> style.withItalic(false) }))
    return this.copy()
}

fun ItemStack.registryName(): ResourceLocation {
    return BuiltInRegistries.ITEM.getKey(this.item) ?: throw IllegalStateException("Item $this has no registry name")
}