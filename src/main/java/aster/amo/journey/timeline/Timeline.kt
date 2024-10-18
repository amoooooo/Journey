package aster.amo.journey.timeline

import aster.amo.ceremony.utils.scheduler.Scheduler
import aster.amo.journey.utils.MolangUtils
import com.bedrockk.molang.runtime.MoLangRuntime
import com.cobblemon.mod.common.api.molang.MoLangFunctions.setup
import com.cobblemon.mod.common.util.asExpression
import com.cobblemon.mod.common.util.asExpressionLike
import com.cobblemon.mod.common.util.resolve
import net.minecraft.server.level.ServerPlayer
import java.lang.reflect.Type
import com.google.gson.*


class Timeline(val timeline: Map<Int, ActionsAtTick> = emptyMap()) {

    companion object {
        val TIMELINES: MutableMap<String, Timeline> = mutableMapOf()

        fun registerTimeline(name: String, timeline: Timeline) {
            TIMELINES[name] = timeline
        }
    }

    fun launch(player: ServerPlayer) {
        timeline.forEach { (tick, actionsAtTick) ->
            when (actionsAtTick) {
                is ActionsAtTick.SingleAction -> {
                    Scheduler.scheduleTask(tick, Scheduler.DelayedAction({
                        actionsAtTick.action.eval(player)
                    }, {}))
                }
                is ActionsAtTick.MultipleActions -> {
                    actionsAtTick.actions.forEach { action ->
                        Scheduler.scheduleTask(tick, Scheduler.DelayedAction({
                            action.eval(player)
                        }, {}))
                    }
                }
            }
        }
    }

    sealed class ActionsAtTick {
        data class SingleAction(val action: Action) : ActionsAtTick()
        data class MultipleActions(val actions: List<Action>) : ActionsAtTick()
    }

    abstract class Action {
        abstract fun eval(player: ServerPlayer)
    }

    data class CommandAction(
        val command: String
    ) : Action() {
        override fun eval(player: ServerPlayer) {
            player.server.commands.dispatcher.execute(
                command.replace("{player}", player.name.string),
                player.server.createCommandSourceStack()
            )
        }
    }

    data class MolangAction(
        val script: String
    ) : Action() {
        override fun eval(player: ServerPlayer) {
            val runtime: MoLangRuntime = MoLangRuntime().setup()
            MolangUtils.setupPlayerStructs(runtime.environment.query, player)
            MolangUtils.setupWorldStructs(runtime.environment.query, player)
            runtime.resolve(script.asExpressionLike())
        }
    }

    class ActionTypeAdapter : JsonSerializer<Action>, JsonDeserializer<Action> {
        override fun serialize(
            src: Action?,
            typeOfSrc: Type?,
            context: JsonSerializationContext?
        ): JsonElement {
            if (src == null) return JsonNull.INSTANCE

            val jsonObject = JsonObject()
            when (src) {
                is CommandAction -> {
                    jsonObject.addProperty("type", "command")
                    jsonObject.addProperty("command", src.command)
                }
                is MolangAction -> {
                    jsonObject.addProperty("type", "molang")
                    jsonObject.addProperty("script", src.script)
                }
                else -> throw JsonParseException("Unknown Action subclass: ${src::class.java}")
            }
            return jsonObject
        }

        override fun deserialize(
            json: JsonElement?,
            typeOfT: Type?,
            context: JsonDeserializationContext?
        ): Action {
            if (json == null || json.isJsonNull) throw JsonParseException("JSON is null or invalid")

            val jsonObject = json.asJsonObject
            val typeElement = jsonObject.get("type") ?: throw JsonParseException("Missing 'type' field in Action JSON")
            val type = typeElement.asString

            return when (type) {
                "command" -> {
                    val command = jsonObject.get("command").asString
                    CommandAction(command)
                }
                "molang" -> {
                    val script = jsonObject.get("script").asString
                    MolangAction(script)
                }
                else -> throw JsonParseException("Unknown Action type: $type")
            }
        }
    }

    class ActionsAtTickTypeAdapter : JsonSerializer<ActionsAtTick>, JsonDeserializer<ActionsAtTick> {
        override fun serialize(
            src: ActionsAtTick?,
            typeOfSrc: Type?,
            context: JsonSerializationContext?
        ): JsonElement {
            if (src == null) return JsonNull.INSTANCE

            return when (src) {
                is ActionsAtTick.SingleAction -> {
                    context?.serialize(src.action)
                }
                is ActionsAtTick.MultipleActions -> {
                    val jsonArray = JsonArray()
                    src.actions.forEach { action ->
                        jsonArray.add(context?.serialize(action))
                    }
                    jsonArray
                }
            } ?: JsonNull.INSTANCE
        }

        override fun deserialize(
            json: JsonElement?,
            typeOfT: Type?,
            context: JsonDeserializationContext?
        ): ActionsAtTick {
            if (json == null || json.isJsonNull) throw JsonParseException("JSON is null or invalid")

            return when {
                json.isJsonObject -> {
                    val action = context?.deserialize<Action>(json, Action::class.java)
                        ?: throw JsonParseException("Failed to deserialize Action")
                    ActionsAtTick.SingleAction(action)
                }
                json.isJsonArray -> {
                    val actions = json.asJsonArray.map { jsonElement ->
                        context?.deserialize<Action>(jsonElement, Action::class.java)
                            ?: throw JsonParseException("Failed to deserialize Action")
                    }
                    ActionsAtTick.MultipleActions(actions)
                }
                else -> throw JsonParseException("Invalid JSON for ActionsAtTick")
            }
        }
    }

    class TimelineTypeAdapter : JsonSerializer<Timeline>, JsonDeserializer<Timeline> {
        override fun serialize(
            src: Timeline?,
            typeOfSrc: Type?,
            context: JsonSerializationContext?
        ): JsonElement {
            if (src == null) return JsonNull.INSTANCE

            val jsonObject = JsonObject()
            val timelineObject = JsonObject()
            src.timeline.forEach { (tick, actionsAtTick) ->
                val tickKey = tick.toString()
                val actionsJson = context?.serialize(actionsAtTick)
                if (actionsJson != null) {
                    timelineObject.add(tickKey, actionsJson)
                }
            }
            jsonObject.add("timeline", timelineObject)
            return jsonObject
        }

        override fun deserialize(
            json: JsonElement?,
            typeOfT: Type?,
            context: JsonDeserializationContext?
        ): Timeline {
            if (json == null || json.isJsonNull) throw JsonParseException("JSON is null or invalid")

            val jsonObject = json.asJsonObject
            val timelineObject = jsonObject.getAsJsonObject("timeline")
                ?: throw JsonParseException("Missing 'timeline' field")

            val timelineMap = mutableMapOf<Int, ActionsAtTick>()
            timelineObject.entrySet().forEach { (tickString, actionsJson) ->
                val tick = tickString.toIntOrNull()
                    ?: throw JsonParseException("Invalid tick value: $tickString")

                val actionsAtTick = context?.deserialize<ActionsAtTick>(actionsJson, ActionsAtTick::class.java)
                    ?: throw JsonParseException("Failed to deserialize ActionsAtTick at tick $tick")

                timelineMap[tick] = actionsAtTick
            }

            return Timeline(timelineMap)
        }
    }
}
