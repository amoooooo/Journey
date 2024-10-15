package aster.amo.journey.task.reward

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

class RewardTypeAdapterFactory : TypeAdapterFactory {
    override fun <T> create(gson: Gson, typeToken: TypeToken<T>): TypeAdapter<T>? {
        if (!Reward::class.java.isAssignableFrom(typeToken.rawType)) {
            return null
        }

        val elementAdapter = gson.getAdapter(JsonElement::class.java)

        return object : TypeAdapter<T>() {
            override fun write(out: JsonWriter, value: T) {
                val jsonObject = JsonObject()
                when (value) {
                    is CommandReward -> {
                        jsonObject.addProperty("type", "command")
                        jsonObject.add("data", gson.toJsonTree(value))
                    }
                    is CurrencyReward -> {
                        jsonObject.addProperty("type", "currency")
                        jsonObject.add("data", gson.toJsonTree(value))
                    }
                    is ScriptReward -> {
                        jsonObject.addProperty("type", "script")
                        jsonObject.add("data", gson.toJsonTree(value))
                    }
                    else -> throw JsonParseException("Unknown reward type: ${value!!::class.java.simpleName}")
                }
                elementAdapter.write(out, jsonObject)
            }

            override fun read(reader: JsonReader): T {
                val jsonElement = elementAdapter.read(reader)
                val jsonObject = jsonElement.asJsonObject

                val type = jsonObject.get("type")?.asString
                    ?: throw JsonParseException("Reward 'type' field is missing")

                val dataElement = jsonObject.get("data")
                    ?: throw JsonParseException("Reward 'data' field is missing")

                val reward: Reward = when (type) {
                    "command" -> {
                        val delegate = gson.getDelegateAdapter(this@RewardTypeAdapterFactory, TypeToken.get(CommandReward::class.java))
                        delegate.fromJsonTree(dataElement)
                    }
                    "currency" -> {
                        val delegate = gson.getDelegateAdapter(this@RewardTypeAdapterFactory, TypeToken.get(CurrencyReward::class.java))
                        delegate.fromJsonTree(dataElement)
                    }
                    "script" -> {
                        val delegate = gson.getDelegateAdapter(this@RewardTypeAdapterFactory, TypeToken.get(ScriptReward::class.java))
                        delegate.fromJsonTree(dataElement)
                    }
                    else -> throw JsonParseException("Unknown reward type: $type")
                }

                @Suppress("UNCHECKED_CAST")
                return reward as T
            }
        }
    }
}

