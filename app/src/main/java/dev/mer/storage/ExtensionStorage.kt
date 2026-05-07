package dev.mer.storage

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Namespaced key-value storage for extensions.
 * Each extension gets its own JSON file to prevent cross-extension data access.
 *
 * This is intentionally simple — a JSON file per extension.
 * For MVP, this avoids adding another Room table + entity + DAO.
 * The file-per-extension model also makes uninstall cleanup trivial.
 */
@Singleton
class ExtensionStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json { prettyPrint = false }
    private val storageDir: File
        get() = File(context.filesDir, "extensions/.storage").also { it.mkdirs() }

    fun get(extensionId: String, key: String): String? {
        val data = readStorage(extensionId)
        return data[key]?.jsonPrimitive?.content
    }

    fun set(extensionId: String, key: String, value: String) {
        val data = readStorage(extensionId).toMutableMap()
        data[key] = JsonPrimitive(value)
        writeStorage(extensionId, JsonObject(data))
    }

    fun remove(extensionId: String, key: String) {
        val data = readStorage(extensionId).toMutableMap()
        data.remove(key)
        writeStorage(extensionId, JsonObject(data))
    }

    fun clearAll(extensionId: String) {
        getStorageFile(extensionId).delete()
    }

    private fun readStorage(extensionId: String): Map<String, kotlinx.serialization.json.JsonElement> {
        val file = getStorageFile(extensionId)
        if (!file.exists()) return emptyMap()
        return try {
            json.decodeFromString<JsonObject>(file.readText())
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun writeStorage(extensionId: String, data: JsonObject) {
        getStorageFile(extensionId).writeText(json.encodeToString(JsonObject.serializer(), data))
    }

    private fun getStorageFile(extensionId: String): File {
        // Sanitize extension ID to prevent path traversal
        val safeId = extensionId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return File(storageDir, "$safeId.json")
    }
}
