package aster.amo.journey.reactive.collections.set

import com.cobblemon.mod.common.api.reactive.Observable
import net.minecraft.nbt.CompoundTag
import aster.amo.journey.reactive.collections.getElementObservables


fun <T> Set<T>.saveToNbt(
    elementKey: String = "element",
    elementHandler: (T) -> CompoundTag,
): CompoundTag {
    val nbt = CompoundTag()
    var size = 0
    this.iterator().forEach { nbt.put(elementKey + size++, elementHandler(it)) }
    nbt.putInt("size", size)
    return nbt
}


fun <T> CompoundTag.loadObservableSetOf(
    elementKey: String = "element",
    loadElementHandler: (CompoundTag) -> T,
    elementObservableHandler: (T) -> Set<Observable<*>> = { it.getElementObservables() },
) = ObservableSet(this.loadSet(elementKey, loadElementHandler), elementObservableHandler)


fun <T> CompoundTag.loadMutableObservableSetOf(
    elementKey: String = "element",
    loadElementHandler: (CompoundTag) -> T,
    elementObservableHandler: (T) -> Set<Observable<*>> = { it.getElementObservables() },
) = MutableObservableSet(this.loadSet(elementKey, loadElementHandler), elementObservableHandler)


private fun <T> CompoundTag.loadSet(
    elementKey: String = "element",
    elementHandler: (CompoundTag) -> T,
): Set<T> {
    val newSet = mutableSetOf<T>()
    if (this.contains("size")) {
        val size = this.getInt("size")
        for (i in 0 until size) {
            newSet.add(elementHandler(this.getCompound(elementKey + i)))
        }
    }
    return newSet
}
