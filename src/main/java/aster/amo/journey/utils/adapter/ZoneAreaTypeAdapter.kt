package aster.amo.journey.utils.adapter

import com.google.gson.*
import aster.amo.journey.zones.area.*
import java.lang.reflect.Type

object ZoneAreaTypeAdapter : JsonSerializer<ZoneArea>, JsonDeserializer<ZoneArea> {
    override fun serialize(src: ZoneArea, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val jsonObject = JsonObject()
        val type = when(src) {
            is ZoneBox -> "box"
            is ZoneSphere -> "sphere"
            is ZoneCylinder -> "cylinder"
            is ZoneShape -> "shape"
            else -> throw IllegalArgumentException("Unknown ZoneArea type: ${src::class.java.simpleName}")
        }
        jsonObject.addProperty("type", type) // Add type information
        jsonObject.add("data", context.serialize(src)) // Serialize the data object itself
        return jsonObject
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ZoneArea? {
        val jsonObject = json.asJsonObject
        val type = jsonObject.get("type").asString
        val data = jsonObject.get("data")

        return when (type) {
            "box" -> context.deserialize(data, ZoneBox::class.java)
            "sphere" -> context.deserialize(data, ZoneSphere::class.java)
            "cylinder" -> context.deserialize(data, ZoneCylinder::class.java)
            "shape" -> context.deserialize(data, ZoneShape::class.java)
            else -> throw JsonParseException("Unknown ZoneArea type: $type")
        }
    }
}