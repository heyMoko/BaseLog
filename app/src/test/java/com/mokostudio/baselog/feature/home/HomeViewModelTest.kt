package com.mokostudio.baselog.feature.home

import com.mokostudio.baselog.core.auth.AuthRepository
import com.mokostudio.baselog.core.user.BaseballTeam
import com.mokostudio.baselog.core.user.UserProfile
import com.mokostudio.baselog.core.user.UserProfileDraft
import com.mokostudio.baselog.core.user.UserProfileRepository
import com.mokostudio.baselog.feature.friends.FriendLeaderboardEntry
import com.mokostudio.baselog.feature.friends.FriendLeaderboardLoadState
import com.mokostudio.baselog.feature.friends.FriendLeaderboardRepository
import com.mokostudio.baselog.feature.log.BaseballGameResult
import com.mokostudio.baselog.feature.log.BaseballLogDraft
import com.mokostudio.baselog.feature.log.BaseballLogEntry
import com.mokostudio.baselog.feature.log.BaseballLogRepository
import com.mokostudio.baselog.feature.log.WinRateSummary
import com.mokostudio.baselog.testutil.MainDispatcherRule
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun signOut_callsRepository() = runTest {
        val repository = FakeAuthRepository()
        val viewModel = HomeViewModel(
            authRepository = repository,
            userProfileRepository = FakeUserProfileRepository(),
            baseballLogRepository = FakeBaseballLogRepository(),
            friendLeaderboardRepository = FakeFriendLeaderboardRepository()
        )

        viewModel.signOut()
        advanceUntilIdle()

        assertTrue(repository.signOutCalled)
    }

    @Test
    fun uiState_reflectsCurrentUserProfile() = runTest {
        val userProfileRepository = FakeUserProfileRepository()
        val viewModel = HomeViewModel(
            authRepository = FakeAuthRepository(),
            userProfileRepository = userProfileRepository,
            baseballLogRepository = FakeBaseballLogRepository(),
            friendLeaderboardRepository = FakeFriendLeaderboardRepository()
        )
        val collectionJob: Job = backgroundScope.launch {
            viewModel.uiState.collect {}
        }

        userProfileRepository.profile.value = UserProfile(
            nickname = "Moko",
            favoriteTeam = BaseballTeam.LgTwins,
            bio = "Tracks weekday games.",
            email = "moko@example.com",
            photoUrl = ""
        )
        advanceUntilIdle()

        assertEquals("Moko", viewModel.uiState.value.profile?.nickname)
        assertEquals("LG Twins", viewModel.uiState.value.profile?.favoriteTeamName)
        assertTrue(viewModel.uiState.value.profile != null)
        assertTrue(!viewModel.uiState.value.isProfileUnavailable)
        collectionJob.cancel()
    }

    @Test
    fun uiState_marksProfileUnavailableWhenRepositoryReturnsNull() = runTest {
        val viewModel = HomeViewModel(
            authRepository = FakeAuthRepository(),
            userProfileRepository = FakeUserProfileRepository(),
            baseballLogRepository = FakeBaseballLogRepository(),
            friendLeaderboardRepository = FakeFriendLeaderboardRepository()
        )
        val collectionJob: Job = backgroundScope.launch {
            viewModel.uiState.collect {}
        }

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isProfileUnavailable)
        assertTrue(viewModel.uiState.value.profile == null)
        collectionJob.cancel()
    }

    @Test
    fun uiState_includesLogSummaryAndRecentLogs() = runTest {
        val userProfileRepository = FakeUserProfileRepository()
        val baseballLogRepository = FakeBaseballLogRepository()
        val currentYear = LocalDate.now().year
        val friendLeaderboardRepository = FakeFriendLeaderboardRepository()
        val viewModel = HomeViewModel(
            authRepository = FakeAuthRepository(),
            userProfileRepository = userProfileRepository,
            baseballLogRepository = baseballLogRepository,
            friendLeaderboardRepository = friendLeaderboardRepository
        )
        val collectionJob: Job = backgroundScope.launch {
            viewModel.uiState.collect {}
        }

        userProfileRepository.profile.value = UserProfile(
            nickname = "Moko",
            favoriteTeam = BaseballTeam.LgTwins,
            bio = "Tracks weekday games.",
            email = "moko@example.com",
            photoUrl = ""
        )
        baseballLogRepository.logs.value = listOf(
            logEntry("1", "$currentYear-07-12", BaseballTeam.DoosanBears, BaseballGameResult.Win),
            logEntry("2", "$currentYear-07-06", BaseballTeam.SsgLanders, BaseballGameResult.Draw),
            logEntry("3", "$currentYear-07-01", BaseballTeam.KtWiz, BaseballGameResult.Loss),
            logEntry("4", "${currentYear - 1}-09-10", BaseballTeam.KiaTigers, BaseballGameResult.Win)
        )
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.logSummary.hasLogs)
        assertEquals(4, viewModel.uiState.value.logSummary.totalGames)
        assertEquals(66, viewModel.uiState.value.logSummary.overallWinRatePercent)
        assertEquals("1W 1L 1D", viewModel.uiState.value.logSummary.currentYearRecord)
        assertEquals(3, viewModel.uiState.value.logSummary.recentLogs.size)
        assertEquals("$currentYear-07-12", viewModel.uiState.value.logSummary.recentLogs.first().attendedDate)
        collectionJob.cancel()
    }

    @Test
    fun uiState_includesFriendLeaderboardPreview() = runTest {
        val userProfileRepository = FakeUserProfileRepository()
        val friendLeaderboardRepository = FakeFriendLeaderboardRepository()
        val viewModel = HomeViewModel(
            authRepository = FakeAuthRepository(),
            userProfileRepository = userProfileRepository,
            baseballLogRepository = FakeBaseballLogRepository(),
            friendLeaderboardRepository = friendLeaderboardRepository
        )
        val collectionJob: Job = backgroundScope.launch {
            viewModel.uiState.collect {}
        }

        userProfileRepository.profile.value = UserProfile(
            nickname = "Moko",
            favoriteTeam = BaseballTeam.LgTwins,
            bio = "",
            email = "moko@example.com",
            photoUrl = ""
        )
        friendLeaderboardRepository.state.value = FriendLeaderboardLoadState(
            entries = listOf(
                FriendLeaderboardEntry(
                    userId = "me",
                    nickname = "Moko",
                    favoriteTeam = BaseballTeam.LgTwins,
                    summary = WinRateSummary(
                        totalGames = 8,
                        wins = 6,
                        losses = 2,
                        draws = 0,
                        winRatePercent = 75
                    ),
                    yearlySummaries = emptyMap(),
                    isCurrentUser = true
                ),
                FriendLeaderboardEntry(
                    userId = "friend-1",
                    nickname = "Jin",
                    favoriteTeam = BaseballTeam.DoosanBears,
                    summary = WinRateSummary(
                        totalGames = 6,
                        wins = 4,
                        losses = 2,
                        draws = 0,
                        winRatePercent = 66
                    ),
                    yearlySummaries = emptyMap(),
                    isCurrentUser = false
                )
            )
        )
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.friendLeaderboardPreview.topEntries.size)
        assertEquals(1, viewModel.uiState.value.friendLeaderboardPreview.myRank)
        assertEquals("Moko", viewModel.uiState.value.friendLeaderboardPreview.topEntries.first().nickname)
        collectionJob.cancel()
    }

    private class FakeAuthRepository : AuthRepository {
        var signOutCalled = false

        override fun observeAuthenticated(): Flow<Boolean> = MutableStateFlow(true)

        override suspend fun signInWithGoogleIdToken(idToken: String): Result<Unit> {
            return Result.success(Unit)
        }

        override suspend fun signOut() {
            signOutCalled = true
        }
    }

    private class FakeUserProfileRepository : UserProfileRepository {
        val profile = MutableStateFlow<UserProfile?>(null)

        override fun observeProfileCompleted(): Flow<Boolean> = MutableStateFlow(false)

        override fun observeCurrentUserProfile(): Flow<UserProfile?> = profile

        override suspend fun saveProfile(profile: UserProfileDraft): Result<Unit> {
            return Result.success(Unit)
        }
    }

    private class FakeBaseballLogRepository : BaseballLogRepository {
        val logs = MutableStateFlow<List<BaseballLogEntry>>(emptyList())

        override fun observeLogs(): Flow<List<BaseballLogEntry>> = logs

        override fun observeLog(logId: String): Flow<BaseballLogEntry?> =
            MutableStateFlow(logs.value.firstOrNull { it.id == logId })

        override suspend fun createLog(log: BaseballLogDraft): Result<Unit> = Result.success(Unit)

        override suspend fun updateLog(
            logId: String,
            log: BaseballLogDraft
        ): Result<Unit> = Result.success(Unit)

        override suspend fun deleteLog(logId: String): Result<Unit> = Result.success(Unit)
    }

    private class FakeFriendLeaderboardRepository : FriendLeaderboardRepository {
        val state = MutableStateFlow(FriendLeaderboardLoadState())

        override fun observeLeaderboard(): Flow<FriendLeaderboardLoadState> = state
    }

    private fun logEntry(
        id: String,
        date: String,
        opponentTeam: BaseballTeam,
        result: BaseballGameResult
    ): BaseballLogEntry {
        return BaseballLogEntry(
            id = id,
            attendedDate = LocalDate.parse(date),
            opponentTeam = opponentTeam,
            result = result
        )
    }
}
