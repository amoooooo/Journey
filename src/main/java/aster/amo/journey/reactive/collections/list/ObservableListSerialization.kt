package aster.amo.journey.reactive.collections.list

import com.cobblemon.mod.common.api.reactive.Observable
import net.minecraft.nbt.CompoundTag
import aster.amo.journey.reactive.collections.getElementObservables


fun <T> List<T>.saveToNbt(
    elementKey: String = "element",
    elementHandler: (T) -> CompoundTag,
): CompoundTag {
    val nbt = CompoundTag()
    var size = 0
    this.iterator().forEach { nbt.put(elementKey + size++, elementHandler(it)) }
    nbt.putInt("size", size)
    return nbt
}


fun <T> CompoundTag.loadObservableListOf(
    elementKey: String = "element",
    loadElementHandler: (CompoundTag) -> T,
    elementObservableHandler: (T) -> Set<Observable<*>> = { it.getElementObservables() },
) = ObservableList(this.loadList(elementKey, loadElementHandler), elementObservableHandler)


fun <T> CompoundTag.loadMutableObservableListOf(
    elementKey: String = "element",
    loadElementHandler: (CompoundTag) -> T,
    elementObservableHandler: (T) -> Set<Observable<*>> = { it.getElementObservables() },
) = MutableObservableList(this.loadList(elementKey, loadElementHandler), elementObservableHandler)


private fun <T> CompoundTag.loadList(
    elementKey: String = "element",
    elementHandler: (CompoundTag) -> T
): List<T> {
    val newList = mutableListOf<T>()
    if (this.contains("size")) {
        val size = this.getInt("size")
        for (i in 0 until size) {
            newList.add(elementHandler(this.getCompound(elementKey + i)))
        }
    }
    return newList
}
