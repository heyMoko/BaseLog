package com.mokostudio.baselog.feature.ranking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mokostudio.baselog.feature.friends.FriendLeaderboardRepository
import com.mokostudio.baselog.feature.friends.FriendLeaderboardUiState
import com.mokostudio.baselog.feature.friends.LeaderboardMetric
import com.mokostudio.baselog.feature.friends.RankedFriendLeaderboardEntry
import com.mokostudio.baselog.feature.friends.toRankedEntries
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@HiltViewModel
class RankingViewModel @Inject constructor(
    friendLeaderboardRepository: FriendLeaderboardRepository
) : ViewModel() {
    private val selectedMetric = MutableStateFlow(LeaderboardMetric.WinRate)
    private val selectedYear = MutableStateFlow<Int?>(null)

    val uiState: StateFlow<RankingUiState> =
        combine(
            friendLeaderboardRepository.observeLeaderboard(),
            selectedMetric,
            selectedYear
        ) { leaderboardState, metric, year ->
            val resolvedYear = year?.takeIf { it in leaderboardState.availableYears }
            val entries = leaderboardState.entries.toRankedEntries(
                metric = metric,
                selectedYear = resolvedYear
            )

            RankingUiState(
                isLoading = false,
                leaderboard = FriendLeaderboardUiState(
                    entries = entries,
                    availableYears = leaderboardState.availableYears,
                    selectedMetric = metric,
                    selectedYear = resolvedYear,
                    errorMessage = leaderboardState.errorMessage
                ),
                myEntry = entries.firstOrNull { it.isCurrentUser }
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = RankingUiState(isLoading = true)
        )

    fun onMetricSelected(metric: LeaderboardMetric) {
        selectedMetric.update { metric }
    }

    fun onYearSelected(year: Int?) {
        selectedYear.update { year }
    }
}

data class RankingUiState(
    val isLoading: Boolean = false,
    val leaderboard: FriendLeaderboardUiState = FriendLeaderboardUiState(),
    val myEntry: RankedFriendLeaderboardEntry? = null
)
