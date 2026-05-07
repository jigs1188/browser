package dev.mer.ui.settings

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.mer.ai.AiService
import dev.mer.storage.AppPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class SettingsUiState(
    val apiKey: String = "",
    val isKeyVisible: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val aiService: AiService,
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        val savedKey = appPreferences.geminiApiKey
        aiService.setApiKey(savedKey)
        _uiState.update { it.copy(apiKey = savedKey) }
    }

    fun onApiKeyChanged(key: String) {
        _uiState.update { it.copy(apiKey = key) }
    }

    fun saveApiKey() {
        val key = _uiState.value.apiKey.trim()
        appPreferences.geminiApiKey = key
        aiService.setApiKey(key)
        _uiState.update { it.copy(message = if (key.isNotEmpty()) "API key saved" else "API key cleared") }
    }

    fun toggleKeyVisibility() {
        _uiState.update { it.copy(isKeyVisible = !it.isKeyVisible) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
