package aster.amo.journey.utils.adapter
import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.io.IOException
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.*


class OptionalTypeAdapter<E>(private val adapter: TypeAdapter<E>) : TypeAdapter<Optional<E>?>() {
    @Throws(IOException::class)
    override fun write(out: JsonWriter, value: Optional<E>?) {
        if (value?.isPresent == true) {
            adapter.write(out, value.get())
        } else {
            out.nullValue()
        }
    }

    @Throws(IOException::class)
    override fun read(`in`: JsonReader): Optional<E> {
        val peek = `in`.peek()
        if (peek != JsonToken.NULL) {
            return Optional.ofNullable(adapter.read(`in`)) as Optional<E>
        }

        `in`.nextNull()
        return Optional.empty<E>() as Optional<E>
    }
}
