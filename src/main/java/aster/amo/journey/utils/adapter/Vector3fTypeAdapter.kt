package aster.amo.journey.utils.adapter

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import org.joml.Vector3f
import java.io.IOException

/**
 * Custom TypeAdapter for serializing and deserializing JOML's Vector3f.
 */
class Vector3fTypeAdapter : TypeAdapter<Vector3f>() {

    @Throws(IOException::class)
    override fun write(out: JsonWriter, vector: Vector3f?) {
        if (vector == null) {
            out.nullValue()
            return
        }

        out.beginObject()
        out.name("x").value(vector.x)
        out.name("y").value(vector.y)
        out.name("z").value(vector.z)
        out.endObject()
    }

    @Throws(IOException::class)
    override fun read(`in`: JsonReader): Vector3f? {
        if (`in`.peek() == com.google.gson.stream.JsonToken.NULL) {
            `in`.nextNull()
            return null
        }

        var x = 0f
        var y = 0f
        var z = 0f

        `in`.beginObject()
        while (`in`.hasNext()) {
            when (`in`.nextName()) {
                "x" -> x = `in`.nextDouble().toFloat()
                "y" -> y = `in`.nextDouble().toFloat()
                "z" -> z = `in`.nextDouble().toFloat()
                else -> `in`.skipValue()
            }
        }
        `in`.endObject()

        return Vector3f(x, y, z)
    }
}
