package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DashboardUiState
import com.example.data.ServerRepository
import com.example.data.StoreCategory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TgzViewModel(
    private val repository: ServerRepository
) : ViewModel() {

    companion object {
        /** Exact 2-minute (120,000 ms) automatic refresh interval per specification. */
        const val AUTO_REFRESH_INTERVAL_MS = 120_000L
        private const val AUTO_REFRESH_SECONDS = 120
    }

    private val refreshMutex = Mutex()
    private var autoRefreshJob: Job? = null
    private var countdownJob: Job? = null

    private val _uiState = MutableStateFlow(
        DashboardUiState(
            isInitialLoading = true,
            isRefreshing = false,
            nextRefreshSeconds = AUTO_REFRESH_SECONDS,
            serverAddress = repository.config.fullServerAddress,
            discordInviteUrl = repository.config.discordInviteUrl
        )
    )
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _playerSearchQuery = MutableStateFlow("")
    val playerSearchQuery: StateFlow<String> = _playerSearchQuery.asStateFlow()

    private val _selectedStoreCategory = MutableStateFlow(StoreCategory.ALL)
    val selectedStoreCategory: StateFlow<StoreCategory> = _selectedStoreCategory.asStateFlow()

    private val _expandedFaqIds = MutableStateFlow(setOf("faq_join"))
    val expandedFaqIds: StateFlow<Set<String>> = _expandedFaqIds.asStateFlow()

    init {
        startAutoRefreshCycle()
    }

    fun refreshNow() {
        viewModelScope.launch {
            performRefresh(resetSchedule = true)
        }
    }

    fun updatePlayerSearch(query: String) {
        _playerSearchQuery.value = query
    }

    fun selectStoreCategory(category: StoreCategory) {
        _selectedStoreCategory.value = category
    }

    fun toggleFaqExpanded(faqId: String) {
        _expandedFaqIds.update { current ->
            if (faqId in current) current - faqId else current + faqId
        }
    }

    private fun startAutoRefreshCycle() {
        autoRefreshJob?.cancel()
        countdownJob?.cancel()

        autoRefreshJob = viewModelScope.launch {
            // Initial fetch immediately on launch
            performRefresh(resetSchedule = false)
            // Strict 120,000 ms (2-minute) polling loop
            while (isActive) {
                delay(AUTO_REFRESH_INTERVAL_MS)
                performRefresh(resetSchedule = false)
            }
        }

        startCountdownTicker()
    }

    private fun startCountdownTicker() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (isActive) {
                delay(1_000L)
                _uiState.update { state ->
                    val next = if (state.isRefreshing) {
                        AUTO_REFRESH_SECONDS
                    } else {
                        (state.nextRefreshSeconds - 1).coerceAtLeast(0)
                    }
                    state.copy(nextRefreshSeconds = next)
                }
            }
        }
    }

    private suspend fun performRefresh(resetSchedule: Boolean) {
        // Prevent simultaneous overlapping refreshes
        if (!refreshMutex.tryLock()) return

        try {
            _uiState.update { it.copy(isRefreshing = true) }

            val snapshot = repository.fetchLiveSnapshot()
            val formattedTime = formatTimestamp(System.currentTimeMillis())

            _uiState.update { current ->
                // Preserve previous valid data if either API fails
                current.copy(
                    isInitialLoading = false,
                    isRefreshing = false,
                    lastUpdatedFormatted = formattedTime,
                    nextRefreshSeconds = AUTO_REFRESH_SECONDS,
                    discordStats = snapshot.discordStats ?: current.discordStats,
                    minecraftStatus = snapshot.minecraftStatus ?: current.minecraftStatus,
                    discordError = snapshot.discordError,
                    minecraftError = snapshot.minecraftError
                )
            }

            if (resetSchedule) {
                // Restart the exact 120,000 ms timer from the manual refresh completion
                autoRefreshJob?.cancel()
                autoRefreshJob = viewModelScope.launch {
                    while (isActive) {
                        delay(AUTO_REFRESH_INTERVAL_MS)
                        performRefresh(resetSchedule = false)
                    }
                }
            }
        } finally {
            _uiState.update { it.copy(isInitialLoading = false, isRefreshing = false) }
            refreshMutex.unlock()
        }
    }

    private fun formatTimestamp(epochMs: Long): String {
        val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return formatter.format(Date(epochMs))
    }

    override fun onCleared() {
        autoRefreshJob?.cancel()
        countdownJob?.cancel()
        super.onCleared()
    }
}
