package aster.amo.journey.utils.adapter

import aster.amo.journey.task.TaskSource
import com.google.gson.*
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import org.joml.Vector3f
import java.lang.reflect.Type

/**
 * Custom TypeAdapter for serializing and deserializing TaskSource objects.
 */
class TaskSourceTypeAdapter(
    private val vector3fAdapter: TypeAdapter<Vector3f>
) : TypeAdapter<TaskSource>() {

    override fun write(out: JsonWriter, taskSource: TaskSource?) {
        if (taskSource == null) {
            out.nullValue()
            return
        }

        out.beginObject()

        // Write 'uuid' field
        out.name("uuid").value(taskSource.uuid)

        // Write 'tasks' field
        out.name("tasks")
        out.beginObject()
        for ((taskId, sourceInfo) in taskSource.tasks) {
            out.name(taskId)
            writeSourceInfo(out, sourceInfo)
        }
        out.endObject()

        // Write 'script' field
        out.name("script").value(taskSource.script)

        // Write 'marker_position' field using the Vector3f TypeAdapter
        out.name("marker_position")
        vector3fAdapter.write(out, taskSource.markerPosition)

        out.endObject()
    }

    /**
     * Helper function to write a SourceInfo object.
     */
    private fun writeSourceInfo(out: JsonWriter, sourceInfo: TaskSource.SourceInfo) {
        out.beginObject()
        out.name("condition").value(sourceInfo.condition)
        out.name("source").value(sourceInfo.source)
        out.endObject()
    }

    override fun read(reader: JsonReader): TaskSource? {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            return null
        }

        var uuid = ""
        val tasks: MutableMap<String, TaskSource.SourceInfo> = mutableMapOf()
        var script = ""
        var markerPosition = Vector3f(0.0f, 2.0f, 0.0f) // Default value

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "uuid" -> uuid = reader.nextString()
                "tasks" -> {
                    reader.beginObject()
                    while (reader.hasNext()) {
                        val taskId = reader.nextName()
                        val sourceInfo = readSourceInfo(reader)
                        tasks[taskId] = sourceInfo
                    }
                    reader.endObject()
                }
                "script" -> script = reader.nextString()
                "marker_position" -> markerPosition = vector3fAdapter.read(reader) ?: Vector3f(0.0f, 2.0f, 0.0f)
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        return TaskSource(
            uuid = uuid,
            tasks = tasks,
            script = script,
            markerPosition = markerPosition
        )
    }

    /**
     * Helper function to read a SourceInfo object.
     */
    private fun readSourceInfo(reader: JsonReader): TaskSource.SourceInfo {
        var condition = ""
        var source = false

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "condition" -> condition = reader.nextString()
                "source" -> source = reader.nextBoolean()
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        return TaskSource.SourceInfo(
            condition = condition,
            source = source
        )
    }
}
