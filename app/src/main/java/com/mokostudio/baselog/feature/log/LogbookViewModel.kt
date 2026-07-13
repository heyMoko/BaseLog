package com.mokostudio.baselog.feature.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LogbookViewModel @Inject constructor(
    private val baseballLogRepository: BaseballLogRepository
) : ViewModel() {
    private val selectedYear = MutableStateFlow<Int?>(null)
    private val pendingDeleteLog = MutableStateFlow<BaseballLogEntry?>(null)
    private val isDeleting = MutableStateFlow(false)
    private val deleteErrorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<LogbookUiState> =
        combine(
            baseballLogRepository.observeLogs(),
            selectedYear,
            pendingDeleteLog,
            isDeleting,
            deleteErrorMessage
        ) { logs, year, deleteLog, deleting, deleteError ->
            val availableYears = logs.map { it.attendedDate.year }
                .distinct()
                .sortedDescending()
            val filteredLogs = year?.let { targetYear ->
                logs.filter { it.attendedDate.year == targetYear }
            } ?: logs

            LogbookUiState(
                logs = filteredLogs,
                availableYears = availableYears,
                selectedYear = year,
                summary = WinRateCalculator.calculate(logs = logs, year = year),
                pendingDeleteLog = deleteLog,
                isDeleting = deleting,
                deleteErrorMessage = deleteError
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LogbookUiState(isLoading = true)
        )

    fun onYearSelected(year: Int?) {
        selectedYear.update { year }
    }

    fun onDeleteClick(log: BaseballLogEntry) {
        pendingDeleteLog.update { log }
        deleteErrorMessage.update { null }
    }

    fun onDeleteDismissed() {
        if (isDeleting.value) return

        pendingDeleteLog.update { null }
        deleteErrorMessage.update { null }
    }

    fun confirmDelete() {
        val log = pendingDeleteLog.value ?: return

        viewModelScope.launch {
            isDeleting.update { true }
            deleteErrorMessage.update { null }

            baseballLogRepository.deleteLog(log.id)
                .onSuccess {
                    pendingDeleteLog.update { null }
                }
                .onFailure { throwable ->
                    deleteErrorMessage.update {
                        throwable.message ?: LOGBOOK_DELETE_ERROR_UNKNOWN
                    }
                }

            isDeleting.update { false }
        }
    }
}

data class LogbookUiState(
    val logs: List<BaseballLogEntry> = emptyList(),
    val availableYears: List<Int> = emptyList(),
    val selectedYear: Int? = null,
    val summary: WinRateSummary = WinRateSummary(),
    val pendingDeleteLog: BaseballLogEntry? = null,
    val isDeleting: Boolean = false,
    val deleteErrorMessage: String? = null,
    val isLoading: Boolean = false
) {
    val isEmpty: Boolean
        get() = !isLoading && logs.isEmpty()
}

private const val LOGBOOK_DELETE_ERROR_UNKNOWN = "We couldn't delete this game log. Try again."
