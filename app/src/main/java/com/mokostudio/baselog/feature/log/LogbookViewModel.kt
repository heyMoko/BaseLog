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

@HiltViewModel
class LogbookViewModel @Inject constructor(
    baseballLogRepository: BaseballLogRepository
) : ViewModel() {
    private val selectedYear = MutableStateFlow<Int?>(null)

    val uiState: StateFlow<LogbookUiState> =
        combine(
            baseballLogRepository.observeLogs(),
            selectedYear
        ) { logs, year ->
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
                summary = WinRateCalculator.calculate(logs = logs, year = year)
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LogbookUiState(isLoading = true)
        )

    fun onYearSelected(year: Int?) {
        selectedYear.update { year }
    }
}

data class LogbookUiState(
    val logs: List<BaseballLogEntry> = emptyList(),
    val availableYears: List<Int> = emptyList(),
    val selectedYear: Int? = null,
    val summary: WinRateSummary = WinRateSummary(),
    val isLoading: Boolean = false
) {
    val isEmpty: Boolean
        get() = !isLoading && logs.isEmpty()
}
