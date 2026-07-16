package com.mokostudio.baselog.feature.ranking

import com.mokostudio.baselog.core.user.BaseballTeam
import com.mokostudio.baselog.feature.friends.FriendLeaderboardEntry
import com.mokostudio.baselog.feature.friends.FriendLeaderboardLoadState
import com.mokostudio.baselog.feature.friends.FriendLeaderboardRepository
import com.mokostudio.baselog.feature.log.WinRateSummary
import com.mokostudio.baselog.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RankingViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun uiState_sortsEntriesBySelectedMetricAndYear() = runTest {
        val repository = FakeFriendLeaderboardRepository()
        repository.state.value = FriendLeaderboardLoadState(
            entries = listOf(
                leaderboardEntry(
                    userId = "me",
                    nickname = "Moko",
                    isCurrentUser = true,
                    overallSummary = summary(
                        totalGames = 8,
                        wins = 6,
                        losses = 2,
                        draws = 0,
                        winRatePercent = 75
                    ),
                    yearlySummaries = mapOf(
                        2026 to summary(
                            totalGames = 3,
                            wins = 1,
                            losses = 2,
                            draws = 0,
                            winRatePercent = 33
                        )
                    )
                ),
                leaderboardEntry(
                    userId = "friend-1",
                    nickname = "Alpha",
                    isCurrentUser = false,
                    overallSummary = summary(
                        totalGames = 12,
                        wins = 7,
                        losses = 3,
                        draws = 2,
                        winRatePercent = 70
                    ),
                    yearlySummaries = mapOf(
                        2026 to summary(
                            totalGames = 5,
                            wins = 4,
                            losses = 1,
                            draws = 0,
                            winRatePercent = 80
                        )
                    )
                )
            ),
            availableYears = listOf(2026)
        )
        val viewModel = RankingViewModel(friendLeaderboardRepository = repository)
        val collectionJob = backgroundScope.launch {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        assertEquals("Moko", viewModel.uiState.value.leaderboard.entries.first().nickname)
        assertEquals(1, viewModel.uiState.value.myEntry?.rank)

        viewModel.onMetricSelected(com.mokostudio.baselog.feature.friends.LeaderboardMetric.TotalGames)
        advanceUntilIdle()

        assertEquals("Alpha", viewModel.uiState.value.leaderboard.entries.first().nickname)
        assertEquals("12", viewModel.uiState.value.leaderboard.entries.first().value)

        viewModel.onYearSelected(2026)
        viewModel.onMetricSelected(com.mokostudio.baselog.feature.friends.LeaderboardMetric.WinRate)
        advanceUntilIdle()

        assertEquals(2026, viewModel.uiState.value.leaderboard.selectedYear)
        assertEquals("Alpha", viewModel.uiState.value.leaderboard.entries.first().nickname)
        assertEquals("80%", viewModel.uiState.value.leaderboard.entries.first().value)
        collectionJob.cancel()
    }

    @Test
    fun onYearSelected_all_restoresOverallRanking() = runTest {
        val repository = FakeFriendLeaderboardRepository()
        repository.state.value = FriendLeaderboardLoadState(
            entries = listOf(
                leaderboardEntry(
                    userId = "me",
                    nickname = "Moko",
                    isCurrentUser = true,
                    overallSummary = summary(
                        totalGames = 10,
                        wins = 7,
                        losses = 3,
                        draws = 0,
                        winRatePercent = 70
                    ),
                    yearlySummaries = mapOf(
                        2026 to summary(
                            totalGames = 2,
                            wins = 0,
                            losses = 2,
                            draws = 0,
                            winRatePercent = 0
                        )
                    )
                ),
                leaderboardEntry(
                    userId = "friend-1",
                    nickname = "Alpha",
                    isCurrentUser = false,
                    overallSummary = summary(
                        totalGames = 8,
                        wins = 5,
                        losses = 3,
                        draws = 0,
                        winRatePercent = 62
                    ),
                    yearlySummaries = mapOf(
                        2026 to summary(
                            totalGames = 4,
                            wins = 4,
                            losses = 0,
                            draws = 0,
                            winRatePercent = 100
                        )
                    )
                )
            ),
            availableYears = listOf(2026)
        )
        val viewModel = RankingViewModel(friendLeaderboardRepository = repository)
        val collectionJob = backgroundScope.launch {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        viewModel.onYearSelected(2026)
        advanceUntilIdle()
        assertEquals("Alpha", viewModel.uiState.value.leaderboard.entries.first().nickname)

        viewModel.onYearSelected(null)
        advanceUntilIdle()

        assertEquals(null, viewModel.uiState.value.leaderboard.selectedYear)
        assertEquals("Moko", viewModel.uiState.value.leaderboard.entries.first().nickname)
        collectionJob.cancel()
    }

    private class FakeFriendLeaderboardRepository : FriendLeaderboardRepository {
        val state = MutableStateFlow(FriendLeaderboardLoadState())

        override fun observeLeaderboard(): Flow<FriendLeaderboardLoadState> = state
    }

    private fun leaderboardEntry(
        userId: String,
        nickname: String,
        isCurrentUser: Boolean,
        overallSummary: WinRateSummary,
        yearlySummaries: Map<Int, WinRateSummary>
    ): FriendLeaderboardEntry {
        return FriendLeaderboardEntry(
            userId = userId,
            nickname = nickname,
            favoriteTeam = BaseballTeam.LgTwins,
            summary = overallSummary,
            yearlySummaries = yearlySummaries,
            isCurrentUser = isCurrentUser
        )
    }

    private fun summary(
        totalGames: Int,
        wins: Int,
        losses: Int,
        draws: Int,
        winRatePercent: Int
    ): WinRateSummary {
        val decidedGames = wins + losses
        return WinRateSummary(
            totalGames = totalGames,
            wins = wins,
            losses = losses,
            draws = draws,
            winRate = if (decidedGames == 0) null else wins.toDouble() / decidedGames.toDouble(),
            winRatePercent = winRatePercent
        )
    }
}
