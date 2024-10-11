package aster.amo.journey.task.event

class JourneyEvent(val name: String, event: () -> Unit) {
    init {
        event()
        register(this)
    }

    companion object {
        private val registry = mutableListOf<JourneyEvent>()

        private fun register(journeyEvent: JourneyEvent) {
            registry.add(journeyEvent)
        }

        fun getAllEvents(): List<JourneyEvent> = registry.toList()
    }
}
