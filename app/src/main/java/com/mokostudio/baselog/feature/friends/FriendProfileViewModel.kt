package com.mokostudio.baselog.feature.friends

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mokostudio.baselog.feature.log.WinRateCalculator
import com.mokostudio.baselog.feature.log.WinRateSummary
import com.mokostudio.baselog.navigation.FRIEND_USER_ID_NAV_ARG
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@HiltViewModel
class FriendProfileViewModel @Inject constructor(
    friendStatsRepository: FriendStatsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val friendUserId: String = checkNotNull(savedStateHandle[FRIEND_USER_ID_NAV_ARG])
    private val selectedYear = MutableStateFlow<Int?>(null)

    val uiState: StateFlow<FriendProfileUiState> =
        combine(
            friendStatsRepository.observeFriendProfile(friendUserId),
            friendStatsRepository.observeFriendLogs(friendUserId),
            selectedYear
        ) { profileState, logsState, selectedYearValue ->
            val profile = profileState.profile
            val logs = logsState.logs
            val errorMessage = listOfNotNull(
                profileState.errorMessage,
                logsState.errorMessage
            ).firstOrNull()

            if (profile == null) {
                FriendProfileUiState(
                    isLoading = false,
                    isFriendUnavailable = true,
                    errorMessage = errorMessage
                )
            } else {
                val availableYears = logs
                    .map { it.attendedDate.year }
                    .distinct()
                    .sortedDescending()
                val resolvedYear = selectedYearValue?.takeIf { it in availableYears }
                    ?: availableYears.firstOrNull()

                FriendProfileUiState(
                    isLoading = false,
                    profile = profile,
                    availableYears = availableYears,
                    selectedYear = resolvedYear,
                    overallSummary = WinRateCalculator.calculate(logs),
                    selectedYearSummary = resolvedYear?.let { year ->
                        WinRateCalculator.calculate(logs = logs, year = year)
                    },
                    recentLogs = logs.take(3),
                    errorMessage = errorMessage
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FriendProfileUiState(isLoading = true)
        )

    fun onYearSelected(year: Int?) {
        selectedYear.update { year }
    }
}

data class FriendProfileUiState(
    val isLoading: Boolean = false,
    val profile: FriendSummary? = null,
    val availableYears: List<Int> = emptyList(),
    val selectedYear: Int? = null,
    val overallSummary: WinRateSummary = WinRateSummary(),
    val selectedYearSummary: WinRateSummary? = null,
    val recentLogs: List<com.mokostudio.baselog.feature.log.BaseballLogEntry> = emptyList(),
    val isFriendUnavailable: Boolean = false,
    val errorMessage: String? = null
)
