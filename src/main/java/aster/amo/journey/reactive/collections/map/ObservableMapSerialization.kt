package aster.amo.journey.reactive.collections.map

import com.cobblemon.mod.common.api.reactive.Observable
import net.minecraft.nbt.CompoundTag
import kotlin.collections.Map.Entry


fun <K, V> Map<K, V>.saveToNbt(
    entryKey: String = "entry",
    entryHandler: (Entry<K, V>) -> CompoundTag,
): CompoundTag {
    val nbt = CompoundTag()
    var size = 0
    @Suppress("UNCHECKED_CAST") // Suppress the warning here
    for (entry in this) {
        nbt.put(entryKey + size++, entryHandler(entry as Entry<K, V>))
    }
    nbt.putInt("size", size)
    return nbt
}


fun  <K, V> CompoundTag.loadObservableMapOf(
    entryKey: String = "entry",
    loadEntryHandler: (CompoundTag) -> Pair<K, V>,
    entryObservableHandler: (K, V) -> Set<Observable<*>> = { k, v -> k.getEntryObservables(v) },
) = ObservableMap(this.loadMap(entryKey, loadEntryHandler), entryObservableHandler)


fun  <K, V> CompoundTag.loadMutableObservableMapOf(
    entryKey: String = "entry",
    loadEntryHandler: (CompoundTag) -> Pair<K, V>,
    entryObservableHandler: (K, V) -> Set<Observable<*>> = { k, v -> k.getEntryObservables(v) },
) = MutableObservableMap(this.loadMap(entryKey, loadEntryHandler), entryObservableHandler)


private fun <K, V> CompoundTag.loadMap(
    entryKey: String = "entry",
    entryHandler: (CompoundTag) -> Pair<K, V>,
): MutableMap<K, V> {
    val newMap: MutableMap<K, V> = mutableMapOf()
    if (this.contains("size")) {
        val size = this.getInt("size")
        for (i in 0 until size) {
            val (key, value) = entryHandler(this.getCompound(entryKey + i))
            newMap[key] = value
        }
    }
    return newMap
}
