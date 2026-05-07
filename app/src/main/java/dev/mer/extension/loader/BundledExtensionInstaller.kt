package dev.mer.extension.loader

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.mer.storage.AppPreferences
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Installs bundled sample extensions from the APK's assets directory
 * on first launch. This gives users something to try immediately
 * without needing to side-load extension folders.
 *
 * Bundled extensions live under assets/extensions/ and are copied
 * to a temp dir, then loaded through the normal ExtensionLoader pipeline.
 * This ensures they go through the same validation as user-installed extensions.
 */
@Singleton
class BundledExtensionInstaller @Inject constructor(
    @ApplicationContext private val context: Context,
    private val extensionLoader: ExtensionLoader,
    private val appPreferences: AppPreferences
) {
    companion object {
        private const val TAG = "BundledInstaller"
        private const val ASSETS_DIR = "extensions"
    }

    /**
     * Install bundled extensions if this is the first launch.
     * Call from Application.onCreate() or a startup initializer.
     */
    suspend fun installIfFirstLaunch() {
        if (!appPreferences.isFirstLaunch) {
            Log.d(TAG, "Not first launch, skipping bundled extension install")
            return
        }

        try {
            val extensionDirs = context.assets.list(ASSETS_DIR) ?: emptyArray()
            Log.d(TAG, "Found ${extensionDirs.size} bundled extension(s)")

            var installed = 0
            for (dirName in extensionDirs) {
                val result = installFromAssets("$ASSETS_DIR/$dirName", dirName)
                if (result) installed++
            }

            Log.i(TAG, "Installed $installed/${extensionDirs.size} bundled extensions")
            appPreferences.isFirstLaunch = false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to install bundled extensions: ${e.message}", e)
            // Don't set isFirstLaunch=false so we retry next launch
        }
    }

    private suspend fun installFromAssets(assetPath: String, dirName: String): Boolean {
        val tempDir = File(context.cacheDir, "bundled_$dirName")
        try {
            tempDir.mkdirs()

            // Copy all files from the asset directory
            val files = context.assets.list(assetPath) ?: return false
            for (fileName in files) {
                val fullPath = "$assetPath/$fileName"
                try {
                    context.assets.open(fullPath).use { input ->
                        File(tempDir, fileName).outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                } catch (e: Exception) {
                    // Could be a subdirectory — skip for now (extensions are flat)
                    Log.d(TAG, "Skipping $fullPath: ${e.message}")
                }
            }

            // Install through the normal pipeline
            val result = extensionLoader.loadFromDirectory(tempDir)
            result.fold(
                onSuccess = { ext ->
                    // Auto-enable bundled extensions
                    extensionLoader.enableAfterInstall(ext.id)
                    Log.i(TAG, "Installed bundled extension: ${ext.name}")
                    return true
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to install '$dirName': ${error.message}")
                    return false
                }
            )
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
