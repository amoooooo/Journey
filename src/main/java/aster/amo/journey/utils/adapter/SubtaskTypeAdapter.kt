package aster.amo.journey.utils.adapter
import aster.amo.journey.task.Subtask
import aster.amo.journey.task.event.JourneyEvent
import aster.amo.journey.task.event.JourneyEvents
import aster.amo.journey.task.reward.Reward
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import org.joml.Vector3i
import java.lang.reflect.Type

class SubtaskTypeAdapter : JsonSerializer<Subtask>, JsonDeserializer<Subtask> {
    override fun serialize(src: Subtask, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val jsonObject = JsonObject()
        jsonObject.addProperty("id", src.id)
        jsonObject.addProperty("name", src.name)
        jsonObject.addProperty("description", src.description)
        jsonObject.addProperty("event", src.event.name)
        jsonObject.add("event_data", src.eventData)
        jsonObject.addProperty("filter", src.filter)
        jsonObject.addProperty("target", src.target)
        jsonObject.add("rewards", context.serialize(src.rewards))
        src.location?.let {
            val locObj = JsonObject()
            locObj.addProperty("x", it.x)
            locObj.addProperty("y", it.y)
            locObj.addProperty("z", it.z)
            jsonObject.add("location", locObj)
        }
        jsonObject.addProperty("script", src.script)
        return jsonObject
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Subtask {
        val jsonObject = json.asJsonObject

        val id = jsonObject.get("id")?.asString ?: ""
        val name = jsonObject.get("name")?.asString ?: ""
        val description = jsonObject.get("description")?.asString ?: ""
        val eventStr = jsonObject.get("event")?.asString ?: "BATTLE_VICTORY"
        val event = JourneyEvent.getAllEvents().firstOrNull { it.name == eventStr } ?: JourneyEvents.BATTLE_VICTORY
        val eventData = jsonObject.getAsJsonObject("event_data") ?: JsonObject()
        val filter = jsonObject.get("filter")?.asString ?: ""
        val target = jsonObject.get("target")?.asDouble ?: 1.0
        val rewards = context.deserialize<List<Reward>>(jsonObject.get("rewards"), object : TypeToken<List<Reward>>() {}.type)
        val location = jsonObject.getAsJsonObject("location")?.let {
            val x = it.get("x")?.asInt ?: 0
            val y = it.get("y")?.asInt ?: 0
            val z = it.get("z")?.asInt ?: 0
            Vector3i(x, y, z)
        }
        val script = jsonObject.get("script")?.asString ?: ""

        return Subtask(
            id = id,
            name = name,
            description = description,
            event = event,
            eventData = Gson().toJsonTree(eventData).asJsonObject,
            filter = filter,
            target = target,
            rewards = rewards,
            location = location,
            script = script
        )
    }
}
