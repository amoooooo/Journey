package aster.amo.journey.utils

import aster.amo.journey.task.Icon
import aster.amo.journey.task.RepeatType
import aster.amo.journey.task.Subtask
import aster.amo.journey.task.Task
import aster.amo.journey.task.reward.Reward
import com.google.gson.*
import net.minecraft.resources.ResourceLocation
import java.lang.reflect.Type

class TaskTypeAdapter : JsonDeserializer<Task>, JsonSerializer<Task> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Task {
        val jsonObject = json.asJsonObject


        val name = jsonObject.get("name")?.asString ?: ""
        val description = jsonObject.getAsJsonArray("description")?.map { it.asString } ?: emptyList()
        val isSequential = jsonObject.get("sequential")?.asBoolean ?: false
        val startRequirement = jsonObject.get("start_requirement")?.asString ?: ""
        val rewards = jsonObject.getAsJsonArray("rewards")?.map {
            context.deserialize<Reward>(it, Reward::class.java)
        } ?: emptyList()
        val icon = jsonObject.get("icon")?.let {
            context.deserialize<Icon>(it, Icon::class.java)
        } ?: Icon()
        val repeatType = jsonObject.get("repeat_type")?.asString?.let {
            try {
                RepeatType.valueOf(it)
            } catch (e: IllegalArgumentException) {
                RepeatType.NONE
            }
        } ?: RepeatType.NONE
        val repeatInterval = jsonObject.get("repeat_interval")?.asInt ?: 0
        val repeatLimit = jsonObject.get("repeat_limit")?.asInt ?: 0
        val tasks = jsonObject.getAsJsonArray("tasks")?.map {
            context.deserialize<Subtask>(it, Subtask::class.java)
        } ?: emptyList()

        return Task(
            name = name,
            description = description,
            isSequential = isSequential,
            startRequirement = startRequirement,
            rewards = rewards,
            icon = icon,
            repeatType = repeatType,
            repeatInterval = repeatInterval,
            repeatLimit = repeatLimit,
            tasks = tasks.toCollection(ArrayDeque())
        )
    }

    override fun serialize(src: Task, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val jsonObject = JsonObject()

        jsonObject.addProperty("name", src.name)
        val descriptionArray = JsonArray()
        src.description.forEach { descriptionArray.add(it) }
        jsonObject.add("description", descriptionArray)
        jsonObject.addProperty("sequential", src.isSequential)
        jsonObject.addProperty("start_requirement", src.startRequirement)
        val rewardsArray = JsonArray()
        src.rewards.forEach { rewardsArray.add(context.serialize(it, Reward::class.java)) }
        jsonObject.add("rewards", rewardsArray)
        jsonObject.add("icon", context.serialize(src.icon, Icon::class.java))
        jsonObject.addProperty("repeat_type", src.repeatType.name)
        jsonObject.addProperty("repeat_interval", src.repeatInterval)
        jsonObject.addProperty("repeat_limit", src.repeatLimit)
        val tasksArray = JsonArray()
        src.tasks.forEach { tasksArray.add(context.serialize(it, Subtask::class.java)) }
        jsonObject.add("tasks", tasksArray)

        return jsonObject
    }
}
