package dev.mer.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.mer.storage.HistoryDao
import dev.mer.storage.HistoryEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyDao: HistoryDao
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val entries: StateFlow<List<HistoryEntry>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                historyDao.observeRecent(200)
            } else {
                historyDao.search(query)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun clearHistory() {
        viewModelScope.launch {
            historyDao.deleteAll()
        }
    }

    fun deleteOlderThan(daysAgo: Int) {
        viewModelScope.launch {
            val cutoff = System.currentTimeMillis() - (daysAgo.toLong() * 24 * 60 * 60 * 1000)
            historyDao.deleteOlderThan(cutoff)
        }
    }
}
