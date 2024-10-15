package aster.amo.journey.utils

import java.util.*
import java.util.concurrent.CountDownLatch

class StructureThread : Thread("Per Player Structure Thread") {
    private val latch = CountDownLatch(1)
    private val whenReady: Queue<() -> Unit> = LinkedList()

    fun launch() {
        this.start()
        this.latch.await()
        for (action in whenReady) {
            action()
        }
    }

    fun queue(action: () -> Unit) {
        if (this.latch.count == 0L) {
            action()
        } else {
            this.whenReady.add(action)
        }
    }

    override fun run() {
        this.latch.countDown()
    }
}