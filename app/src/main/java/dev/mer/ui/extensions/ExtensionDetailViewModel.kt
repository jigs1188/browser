package dev.mer.ui.extensions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.mer.extension.model.Extension
import dev.mer.extension.repository.ExtensionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExtensionDetailViewModel @Inject constructor(
    private val repository: ExtensionRepository
) : ViewModel() {

    fun getExtension(id: String): Flow<Extension?> {
        return repository.observeById(id)
    }

    fun toggleEnabled(extensionId: String, enabled: Boolean) {
        viewModelScope.launch {
            repository.setEnabled(extensionId, enabled)
        }
    }
}
