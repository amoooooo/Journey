package aster.amo.journey.config

import com.cobblemon.mod.common.util.asResource
import com.google.gson.stream.JsonReader
import net.minecraft.resources.ResourceLocation
import aster.amo.journey.Journey
import aster.amo.journey.config.zones.ZoneConfig
import aster.amo.journey.task.Task
import aster.amo.journey.task.TaskRegistry
import aster.amo.journey.utils.Utils
import aster.amo.journey.zones.Zone
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

object ConfigManager {
    private var assetPackage = "assets/${Journey.MOD_ID}"

    lateinit var CONFIG: aster.amo.journey.config.JourneyConfig
    lateinit var ZONE_CONFIG: ZoneConfig

    fun load() {
        // Load defaulted configs if they do not exist
        copyDefaults()

        // Load all files
        CONFIG = loadFile("config.json", aster.amo.journey.config.JourneyConfig())
        ZONE_CONFIG = loadFile("zones.json", ZoneConfig()).also { it ->
            val zoneDir = File(Journey.INSTANCE.configDir, "zones")
            it.zones.addAll(loadZoneConfigs(zoneDir))
        }
        TaskRegistry.clear()
        loadTasks(File(Journey.INSTANCE.configDir, "tasks")).forEach { (rl, task) -> TaskRegistry.registerTask(rl, task) }
    }

    private fun copyDefaults() {
        val classLoader = Journey::class.java.classLoader

        Journey.INSTANCE.configDir.mkdirs()

        attemptDefaultFileCopy(classLoader, "config.json")
    }

    fun loadTasks(directory: File): MutableMap<ResourceLocation, Task> {
        val tasks = mutableMapOf<ResourceLocation, Task>()
        if (directory.exists() && directory.isDirectory) {
            loadConfigsRecursive(directory, tasks) { file ->
                val taskName = "journey:"+file.nameWithoutExtension
                try {
                    var path = file.toPath()
                    // remove Islander.INSTANCE.configDir from the path
                    path = path.subpath(Journey.INSTANCE.configDir.toPath().nameCount, path.nameCount)
                    val particleConfig = loadFile(path.toString(), Task())
                    Pair(taskName.asResource(), particleConfig)
                } catch (e: Exception) {
                    Journey.LOGGER.error("Error loading task config from ${file.absolutePath}", e)
                    Pair(taskName.asResource(), Task())
                }
            }
        }
        return tasks
    }

    fun loadZoneConfigs(directory: File): MutableList<Zone> {
        val zones = mutableListOf<Zone>()
        if (directory.exists() && directory.isDirectory) {
            loadConfigsRecursive<Zone>(directory, zones) { file ->
                try {
                    var path = file.toPath()
                    // remove Islander.INSTANCE.configDir from the path
                    path = path.subpath(Journey.INSTANCE.configDir.toPath().nameCount, path.nameCount)
                    val zone = loadFile(path.toString(), Zone())
                    zone.also { zone ->
                        zone.areas.forEach { area ->
                            area.functions.forEach {
                                it.function(area)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Journey.LOGGER.error("Error loading zone config from ${file.absolutePath}", e)
                    Zone()
                }
            }
        }
        return zones
    }

    private fun <T> loadConfigsRecursive(directory: File, list: MutableList<T>, loadAction: (File) -> T) {
        directory.listFiles()?.forEach { file ->
            if (file.isFile && file.name.endsWith(".json")) {
                list.add(loadAction(file))
            } else if (file.isDirectory) {
                loadConfigsRecursive(file, list, loadAction)
            }
        }
    }

    private fun <K, V> loadConfigsRecursive(directory: File, map: MutableMap<K, V>, loadAction: (File) -> Pair<K, V>) {
        directory.listFiles()?.forEach { file ->
            if (file.isFile && file.name.endsWith(".json")) {
                val (key, value) = loadAction(file)
                map[key] = value
            } else if (file.isDirectory) {
                loadConfigsRecursive(file, map, loadAction)
            }
        }
    }

    fun <T : Any> loadFile(filename: String, default: T, create: Boolean = false): T {
        val file = File(Journey.INSTANCE.configDir, filename)
        var value: T = default
        try {
            Files.createDirectories(Journey.INSTANCE.configDir.toPath())
            if (file.exists()) {
                FileReader(file).use { reader ->
                    val jsonReader = JsonReader(reader)
                    value = Journey.INSTANCE.gsonPretty.fromJson(jsonReader, default::class.java)
                }
            } else if (create) {
                Files.createFile(file.toPath())
                FileWriter(file).use { fileWriter ->
                    fileWriter.write(Journey.INSTANCE.gsonPretty.toJson(default))
                    fileWriter.flush()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return value
    }

    fun <T> saveFile(filename: String, `object`: T): Boolean {
        val dir = Journey.INSTANCE.configDir
        val file = File(dir, filename)
        try {
            FileWriter(file).use { fileWriter ->
                fileWriter.write(Journey.INSTANCE.gsonPretty.toJson(`object`))
                fileWriter.flush()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return true
    }

    private fun attemptDefaultFileCopy(classLoader: ClassLoader, fileName: String) {
        val file = Journey.INSTANCE.configDir.resolve(fileName)
        if (!file.exists()) {
            try {
                val stream = classLoader.getResourceAsStream("${assetPackage}/$fileName")
                    ?: throw NullPointerException("File not found $fileName")

                Files.copy(stream, file.toPath(), StandardCopyOption.REPLACE_EXISTING)
            } catch (e: Exception) {
                Utils.printError("Failed to copy the default file '$fileName': $e")
            }
        }
    }

    private fun attemptDefaultDirectoryCopy(classLoader: ClassLoader, directoryName: String) {
        val directory = Journey.INSTANCE.configDir.resolve(directoryName)
        if (!directory.exists()) {
            directory.mkdirs()
            try {
                val sourceUrl = classLoader.getResource("${assetPackage}/$directoryName")
                    ?: throw NullPointerException("Directory not found $directoryName")
                val sourcePath = Paths.get(sourceUrl.toURI())

                Files.walk(sourcePath).use { stream ->
                    stream.filter { Files.isRegularFile(it) }
                        .forEach { sourceFile ->
                            val destinationFile = directory.resolve(sourcePath.relativize(sourceFile).toString())
                            Files.copy(sourceFile, destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                        }
                }
            } catch (e: Exception) {
                Utils.printError("Failed to copy the default directory '$directoryName': " + e.message)
            }
        }
    }
}
