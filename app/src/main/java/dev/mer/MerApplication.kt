package dev.mer

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dev.mer.ai.AiService
import dev.mer.extension.loader.BundledExtensionInstaller
import dev.mer.storage.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MerApplication : Application() {

    @Inject lateinit var aiService: AiService
    @Inject lateinit var appPreferences: AppPreferences
    @Inject lateinit var bundledInstaller: BundledExtensionInstaller

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // Restore saved API key on app start
        val savedKey = appPreferences.geminiApiKey
        if (savedKey.isNotBlank()) {
            aiService.setApiKey(savedKey)
        }

        // Install bundled extensions on first launch
        appScope.launch {
            bundledInstaller.installIfFirstLaunch()
        }
    }
}
