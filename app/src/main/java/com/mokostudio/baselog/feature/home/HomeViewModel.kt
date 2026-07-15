package com.mokostudio.baselog.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mokostudio.baselog.core.auth.AuthRepository
import com.mokostudio.baselog.core.user.UserProfileRepository
import com.mokostudio.baselog.feature.friends.FriendLeaderboardLoadState
import com.mokostudio.baselog.feature.friends.FriendLeaderboardRepository
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
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    userProfileRepository: UserProfileRepository,
    baseballLogRepository: BaseballLogRepository,
    friendLeaderboardRepository: FriendLeaderboardRepository
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> =
        combine(
            userProfileRepository.observeCurrentUserProfile(),
            baseballLogRepository.observeLogs(),
            friendLeaderboardRepository.observeLeaderboard()
        ) { profile, logs, leaderboard ->
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
                        favoriteTeamName = profile.favoriteTeam.displayName,
                        bio = profile.bio,
                        email = profile.email
                    ),
                    logSummary = HomeLogSummary(
                        totalGames = logs.size,
                        overallWinRatePercent = overallSummary.winRatePercent,
                        overallRecord = overallSummary.toRecordText(),
                        overallMessage = overallSummary.message,
                        currentYear = currentYear,
                        currentYearWinRatePercent = yearlySummary.winRatePercent,
                        currentYearRecord = yearlySummary.toRecordText(),
                        hasLogs = overallSummary.hasGames,
                        recentLogs = logs.take(3).map { log ->
                            HomeRecentLog(
                                id = log.id,
                                attendedDate = log.attendedDate.toString(),
                                opponentTeamName = log.opponentTeam.displayName,
                                resultLabel = log.result.toLabel()
                            )
                        }
                    ),
                    friendLeaderboardPreview = leaderboard.toHomePreview()
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(isLoading = true)
        )

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }
}

data class HomeUiState(
    val isLoading: Boolean = false,
    val profile: HomeProfileSummary? = null,
    val logSummary: HomeLogSummary = HomeLogSummary(),
    val friendLeaderboardPreview: HomeFriendLeaderboardPreview = HomeFriendLeaderboardPreview(),
    val isProfileUnavailable: Boolean = false
)

data class HomeProfileSummary(
    val nickname: String,
    val favoriteTeamName: String,
    val bio: String,
    val email: String
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

data class HomeFriendLeaderboardPreview(
    val topEntries: List<HomeFriendLeaderboardEntry> = emptyList(),
    val myRank: Int? = null,
    val errorMessage: String? = null
) {
    val hasEntries: Boolean
        get() = topEntries.isNotEmpty()
}

data class HomeFriendLeaderboardEntry(
    val rank: Int,
    val nickname: String,
    val favoriteTeamName: String,
    val winRatePercent: Int?,
    val record: String,
    val isCurrentUser: Boolean
)

data class HomeRecentLog(
    val id: String,
    val attendedDate: String,
    val opponentTeamName: String,
    val resultLabel: String
)

private fun FriendLeaderboardLoadState.toHomePreview(): HomeFriendLeaderboardPreview {
    val rankedEntries = entries
        .map { entry ->
            RankedHomeFriendEntry(
                nickname = entry.nickname,
                favoriteTeamName = entry.favoriteTeam.displayName,
                winRatePercent = entry.summary.winRatePercent,
                record = "${entry.summary.wins}W ${entry.summary.losses}L ${entry.summary.draws}D",
                isCurrentUser = entry.isCurrentUser,
                primaryValue = entry.summary.winRate ?: -1.0,
                secondaryValue = entry.summary.totalGames,
                tertiaryValue = entry.summary.wins
            )
        }
        .sortedWith(
            compareByDescending<RankedHomeFriendEntry> { it.primaryValue }
                .thenByDescending { it.secondaryValue }
                .thenByDescending { it.tertiaryValue }
                .thenBy { it.nickname.lowercase() }
        )
        .mapIndexed { index, entry ->
            HomeFriendLeaderboardEntry(
                rank = index + 1,
                nickname = entry.nickname,
                favoriteTeamName = entry.favoriteTeamName,
                winRatePercent = entry.winRatePercent,
                record = entry.record,
                isCurrentUser = entry.isCurrentUser
            )
        }

    return HomeFriendLeaderboardPreview(
        topEntries = rankedEntries.take(3),
        myRank = rankedEntries.firstOrNull { it.isCurrentUser }?.rank,
        errorMessage = errorMessage
    )
}

private data class RankedHomeFriendEntry(
    val nickname: String,
    val favoriteTeamName: String,
    val winRatePercent: Int?,
    val record: String,
    val isCurrentUser: Boolean,
    val primaryValue: Double,
    val secondaryValue: Int,
    val tertiaryValue: Int
)

private fun com.mokostudio.baselog.feature.log.WinRateSummary.toRecordText(): String? {
    if (!hasGames) return null

    return "${wins}W ${losses}L ${draws}D"
}

private fun BaseballGameResult.toLabel(): String = when (this) {
    BaseballGameResult.Win -> "Win"
    BaseballGameResult.Loss -> "Loss"
    BaseballGameResult.Draw -> "Draw"
}
