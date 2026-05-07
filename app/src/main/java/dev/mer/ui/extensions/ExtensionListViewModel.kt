package dev.mer.ui.extensions

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.mer.extension.loader.ExtensionLoader
import dev.mer.extension.model.Extension
import dev.mer.extension.repository.ExtensionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ExtensionListViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val extensionRepository: ExtensionRepository,
    private val extensionLoader: ExtensionLoader
) : ViewModel() {

    val extensions: StateFlow<List<Extension>> =
        extensionRepository.observeAll().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun toggleExtension(extension: Extension) {
        viewModelScope.launch {
            extensionRepository.setEnabled(extension.id, !extension.isEnabled)
        }
    }

    fun uninstallExtension(extension: Extension) {
        viewModelScope.launch {
            extensionRepository.uninstall(extension)
            _message.value = "${extension.name} uninstalled"
        }
    }

    /**
     * Install an extension from a SAF (Storage Access Framework) URI.
     * Copies directory contents to a temp dir, then uses ExtensionLoader.
     */
    fun installFromUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val tempDir = File(context.cacheDir, "ext_install_${System.currentTimeMillis()}")
                tempDir.mkdirs()

                // Copy files from SAF URI to temp directory
                val docFile = DocumentFile.fromTreeUri(context, uri)
                if (docFile == null || !docFile.isDirectory) {
                    _message.value = "Invalid directory selected"
                    return@launch
                }

                copyDocumentTree(docFile, tempDir)

                // Load via ExtensionLoader
                val result = extensionLoader.loadFromDirectory(tempDir)
                result.fold(
                    onSuccess = { ext ->
                        _message.value = "${ext.name} installed successfully"
                    },
                    onFailure = { error ->
                        _message.value = "Install failed: ${error.message}"
                    }
                )

                // Cleanup temp directory
                tempDir.deleteRecursively()
            } catch (e: Exception) {
                _message.value = "Install failed: ${e.message}"
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    private fun copyDocumentTree(source: DocumentFile, destDir: File) {
        source.listFiles().forEach { file ->
            if (file.isFile) {
                val destFile = File(destDir, file.name ?: return@forEach)
                context.contentResolver.openInputStream(file.uri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } else if (file.isDirectory) {
                val subDir = File(destDir, file.name ?: return@forEach)
                subDir.mkdirs()
                copyDocumentTree(file, subDir)
            }
        }
    }
}
