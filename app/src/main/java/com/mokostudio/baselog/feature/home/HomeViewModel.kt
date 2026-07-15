package com.mokostudio.baselog.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mokostudio.baselog.core.user.UserProfileRepository
import com.mokostudio.baselog.feature.log.BaseballGameResult
import com.mokostudio.baselog.feature.log.BaseballLogRepository
import com.mokostudio.baselog.feature.log.WinRateCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class HomeViewModel @Inject constructor(
    userProfileRepository: UserProfileRepository,
    baseballLogRepository: BaseballLogRepository
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> =
        combine(
            userProfileRepository.observeCurrentUserProfile(),
            baseballLogRepository.observeLogs()
        ) { profile, logs ->
            if (profile == null) {
                HomeUiState(
                    isLoading = false,
                    isProfileUnavailable = true
                )
            } else {
                val currentYear = LocalDate.now().year
                val overallSummary = WinRateCalculator.calculate(logs = logs)
                val yearlySummary = WinRateCalculator.calculate(logs = logs, year = currentYear)

                HomeUiState(
                    isLoading = false,
                    profile = HomeProfileSummary(
                        nickname = profile.nickname,
                        favoriteTeamName = profile.favoriteTeam.displayName
                    ),
                    logSummary = HomeLogSummary(
                        totalGames = logs.size,
                        overallWinRatePercent = overallSummary.winRatePercent,
                        overallRecord = overallSummary.toRecordText(),
                        overallMessage = overallSummary.message,
                        currentYear = currentYear,
                        currentYearWinRatePercent = yearlySummary.winRatePercent,
                        currentYearRecord = yearlySummary.toRecordText(),
                        recentLogs = logs.take(2).map { log ->
                            HomeRecentLog(
                                id = log.id,
                                attendedDate = log.attendedDate.toString(),
                                opponentTeamName = log.opponentTeam.displayName,
                                result = log.result
                            )
                        },
                        hasLogs = overallSummary.hasGames
                    )
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(isLoading = true)
        )
}

data class HomeUiState(
    val isLoading: Boolean = false,
    val profile: HomeProfileSummary? = null,
    val logSummary: HomeLogSummary = HomeLogSummary(),
    val isProfileUnavailable: Boolean = false
)

data class HomeProfileSummary(
    val nickname: String,
    val favoriteTeamName: String
)

data class HomeLogSummary(
    val totalGames: Int = 0,
    val overallWinRatePercent: Int? = null,
    val overallRecord: String? = null,
    val overallMessage: String? = null,
    val currentYear: Int = LocalDate.now().year,
    val currentYearWinRatePercent: Int? = null,
    val currentYearRecord: String? = null,
    val recentLogs: List<HomeRecentLog> = emptyList(),
    val hasLogs: Boolean = false
)

data class HomeRecentLog(
    val id: String,
    val attendedDate: String,
    val opponentTeamName: String,
    val result: BaseballGameResult
)

private fun com.mokostudio.baselog.feature.log.WinRateSummary.toRecordText(): String? {
    if (!hasGames) return null

    return "${wins}W ${losses}L ${draws}D"
}
