package dev.mer.storage

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages extension files on disk — install, read, and delete.
 * All extensions are stored under the app's internal storage for security.
 */
@Singleton
class ExtensionFileManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val extensionsDir: File
        get() = File(context.filesDir, "extensions").also { it.mkdirs() }

    fun getExtensionDir(extensionId: String): File {
        val safeId = extensionId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return File(extensionsDir, safeId)
    }

    /**
     * Copies an extension from a source directory to internal storage.
     * Returns the destination directory path.
     */
    fun installFromDirectory(extensionId: String, sourceDir: File): Result<File> {
        return try {
            val destDir = getExtensionDir(extensionId)
            if (destDir.exists()) {
                destDir.deleteRecursively()
            }
            sourceDir.copyRecursively(destDir, overwrite = true)
            Result.success(destDir)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun readFile(extensionId: String, fileName: String): String? {
        val file = File(getExtensionDir(extensionId), fileName)
        return if (file.exists() && file.isFile) file.readText() else null
    }

    fun readManifest(extensionDir: File): String? {
        val file = File(extensionDir, "manifest.json")
        return if (file.exists()) file.readText() else null
    }

    fun fileExists(extensionId: String, fileName: String): Boolean {
        return File(getExtensionDir(extensionId), fileName).exists()
    }

    fun deleteExtension(extensionId: String): Boolean {
        return getExtensionDir(extensionId).deleteRecursively()
    }
}
