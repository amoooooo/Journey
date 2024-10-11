package aster.amo.journey.utils

import aster.amo.journey.task.event.JourneyEvent
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type

class JourneyEventDeserializer : JsonDeserializer<JourneyEvent> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): JourneyEvent {
        val eventName = json.asString
        val event = JourneyEvent.getAllEvents().find { it.name == eventName }
            ?: throw JsonParseException("Unknown event: $eventName")
        return event
    }
}
