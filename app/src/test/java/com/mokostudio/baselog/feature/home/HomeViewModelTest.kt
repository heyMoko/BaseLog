package com.mokostudio.baselog.feature.home

import com.mokostudio.baselog.core.user.BaseballTeam
import com.mokostudio.baselog.core.user.UserProfile
import com.mokostudio.baselog.core.user.UserProfileDraft
import com.mokostudio.baselog.core.user.UserProfileRepository
import com.mokostudio.baselog.feature.log.BaseballGameResult
import com.mokostudio.baselog.feature.log.BaseballLogDraft
import com.mokostudio.baselog.feature.log.BaseballLogEntry
import com.mokostudio.baselog.feature.log.BaseballLogRepository
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
    fun uiState_reflectsCurrentUserProfile() = runTest {
        val userProfileRepository = FakeUserProfileRepository()
        val viewModel = HomeViewModel(
            userProfileRepository = userProfileRepository,
            baseballLogRepository = FakeBaseballLogRepository()
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
            userProfileRepository = FakeUserProfileRepository(),
            baseballLogRepository = FakeBaseballLogRepository()
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
        val viewModel = HomeViewModel(
            userProfileRepository = userProfileRepository,
            baseballLogRepository = baseballLogRepository
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
        assertEquals("1승 1패 1무", viewModel.uiState.value.logSummary.currentYearRecord)
        assertEquals(2, viewModel.uiState.value.logSummary.recentLogs.size)
        assertEquals("$currentYear-07-12", viewModel.uiState.value.logSummary.recentLogs.first().attendedDate)
        collectionJob.cancel()
    }

    @Test
    fun uiState_usesEmptyValuesWhenThereAreNoLogs() = runTest {
        val userProfileRepository = FakeUserProfileRepository()
        val viewModel = HomeViewModel(
            userProfileRepository = userProfileRepository,
            baseballLogRepository = FakeBaseballLogRepository()
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
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.logSummary.totalGames)
        assertEquals(null, viewModel.uiState.value.logSummary.overallWinRatePercent)
        assertTrue(viewModel.uiState.value.logSummary.recentLogs.isEmpty())
        assertTrue(!viewModel.uiState.value.logSummary.hasLogs)
        collectionJob.cancel()
    }

    private class FakeUserProfileRepository : UserProfileRepository {
        val profile = MutableStateFlow<UserProfile?>(null)

        override fun observeProfileCompleted(): Flow<Boolean> = MutableStateFlow(false)

        override fun observeCurrentUserProfile(): Flow<UserProfile?> = profile

        override suspend fun saveProfile(profile: UserProfileDraft): Result<Unit> {
            return Result.success(Unit)
        }

        override suspend fun syncCurrentPublicProfile(): Result<Unit> {
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
