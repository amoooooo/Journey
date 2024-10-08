package soul.software.$mod_id$.config

import com.google.gson.stream.JsonReader
import soul.software.$mod_id$.$mod_name$
import soul.software.$mod_id$.utils.Utils
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

object ConfigManager {
    private var assetPackage = "assets/${$mod_name$.MOD_ID}"

    lateinit var CONFIG: $mod_name$Config

    fun load() {
        // Load defaulted configs if they do not exist
        copyDefaults()

        // Load all files
        CONFIG = loadFile("config.json", $mod_name$Config())
    }

    private fun copyDefaults() {
        val classLoader = $mod_name$::class.java.classLoader

        $mod_name$.INSTANCE.configDir.mkdirs()

        attemptDefaultFileCopy(classLoader, "config.json")
    }

    fun <T : Any> loadFile(filename: String, default: T, create: Boolean = false): T {
        val file = File($mod_name$.INSTANCE.configDir, filename)
        var value: T = default
        try {
            Files.createDirectories($mod_name$.INSTANCE.configDir.toPath())
            if (file.exists()) {
                FileReader(file).use { reader ->
                    val jsonReader = JsonReader(reader)
                    value = $mod_name$.INSTANCE.gsonPretty.fromJson(jsonReader, default::class.java)
                }
            } else if (create) {
                Files.createFile(file.toPath())
                FileWriter(file).use { fileWriter ->
                    fileWriter.write($mod_name$.INSTANCE.gsonPretty.toJson(default))
                    fileWriter.flush()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return value
    }

    fun <T> saveFile(filename: String, `object`: T): Boolean {
        val dir = $mod_name$.INSTANCE.configDir
        val file = File(dir, filename)
        try {
            FileWriter(file).use { fileWriter ->
                fileWriter.write($mod_name$.INSTANCE.gsonPretty.toJson(`object`))
                fileWriter.flush()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return true
    }

    private fun attemptDefaultFileCopy(classLoader: ClassLoader, fileName: String) {
        val file = $mod_name$.INSTANCE.configDir.resolve(fileName)
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
        val directory = $mod_name$.INSTANCE.configDir.resolve(directoryName)
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