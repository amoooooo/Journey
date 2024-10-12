package aster.amo.journey.reactive.collections.set

import aster.amo.journey.reactive.collections.ObservableCollectionIterator


open class ObservableSetIterator<T>(
    protected var iterator: Iterator<T>
) : ObservableCollectionIterator<T, Iterator<T>> {

    override fun hasNext():Boolean = iterator.hasNext()
    override fun next(): T = iterator.next()
}
