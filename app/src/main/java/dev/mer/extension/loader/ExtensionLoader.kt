package dev.mer.extension.loader

import dev.mer.extension.model.Extension
import dev.mer.extension.model.ExtensionManifest
import dev.mer.extension.parser.ManifestParser
import dev.mer.extension.repository.ExtensionRepository
import dev.mer.storage.ExtensionFileManager
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates the extension installation pipeline:
 * 1. Read manifest.json from source directory
 * 2. Parse and validate manifest
 * 3. Verify all referenced files exist
 * 4. Copy to internal storage
 * 5. Create Extension entity
 * 6. Persist to database
 */
@Singleton
class ExtensionLoader @Inject constructor(
    private val manifestParser: ManifestParser,
    private val fileManager: ExtensionFileManager,
    private val repository: ExtensionRepository
) {
    private val json = Json

    suspend fun loadFromDirectory(sourceDir: File): Result<Extension> {
        // Step 1: Read manifest
        val manifestJson = fileManager.readManifest(sourceDir)
            ?: return Result.failure(Exception("No manifest.json found in ${sourceDir.name}"))

        // Step 2: Parse and validate
        val manifest = manifestParser.parse(manifestJson).getOrElse { return Result.failure(it) }

        // Step 3: Verify referenced files exist
        val missingFiles = verifyFiles(sourceDir, manifest)
        if (missingFiles.isNotEmpty()) {
            return Result.failure(
                Exception("Missing files: ${missingFiles.joinToString(", ")}")
            )
        }

        // Step 4: Generate ID and copy to internal storage
        val extensionId = "ext_${UUID.randomUUID().toString().take(8)}"
        val destDir = fileManager.installFromDirectory(extensionId, sourceDir)
            .getOrElse { return Result.failure(it) }

        // Step 5: Create Extension entity
        val extension = Extension(
            id = extensionId,
            name = manifest.name,
            version = manifest.version,
            description = manifest.description,
            matchPatterns = json.encodeToString(manifest.matches),
            scripts = json.encodeToString(manifest.scripts),
            styles = json.encodeToString(manifest.styles),
            permissions = json.encodeToString(manifest.permissions),
            runAt = manifest.runAt,
            directoryPath = destDir.absolutePath,
            isEnabled = false
        )

        // Step 6: Persist
        repository.install(extension)

        return Result.success(extension)
    }

    private fun verifyFiles(dir: File, manifest: ExtensionManifest): List<String> {
        val missing = mutableListOf<String>()
        (manifest.scripts + manifest.styles).forEach { fileName ->
            if (!File(dir, fileName).exists()) {
                missing.add(fileName)
            }
        }
        return missing
    }

    /** Enable an extension immediately after install (used for bundled extensions) */
    suspend fun enableAfterInstall(extensionId: String) {
        repository.setEnabled(extensionId, true)
    }
}
