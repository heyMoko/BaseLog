package com.mokostudio.baselog.feature.friends

import androidx.lifecycle.SavedStateHandle
import com.mokostudio.baselog.core.user.BaseballTeam
import com.mokostudio.baselog.feature.log.BaseballGameResult
import com.mokostudio.baselog.feature.log.BaseballLogEntry
import com.mokostudio.baselog.navigation.FRIEND_USER_ID_NAV_ARG
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
class FriendProfileViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun uiState_showsFriendStatsAndRecentLogs() = runTest {
        val repository = FakeFriendStatsRepository()
        val viewModel = FriendProfileViewModel(
            friendStatsRepository = repository,
            savedStateHandle = SavedStateHandle(
                mapOf(FRIEND_USER_ID_NAV_ARG to "friend-1")
            )
        )
        val collectionJob: Job = backgroundScope.launch {
            viewModel.uiState.collect {}
        }

        repository.profile.value = FriendProfileLoadState(
            profile = FriendSummary(
                userId = "friend-1",
                nickname = "Moko",
                favoriteTeam = BaseballTeam.LgTwins,
                bio = "Jamsil regular.",
                photoUrl = ""
            )
        )
        repository.logsState.value = FriendLogsLoadState(
            logs = listOf(
                logEntry("1", "2026-07-12", BaseballTeam.DoosanBears, BaseballGameResult.Win),
                logEntry("2", "2026-07-06", BaseballTeam.SsgLanders, BaseballGameResult.Draw),
                logEntry("3", "2026-07-01", BaseballTeam.KtWiz, BaseballGameResult.Loss),
                logEntry("4", "2025-09-10", BaseballTeam.KiaTigers, BaseballGameResult.Win)
            )
        )
        advanceUntilIdle()

        assertEquals("Moko", viewModel.uiState.value.profile?.nickname)
        assertEquals(listOf(2026, 2025), viewModel.uiState.value.availableYears)
        assertEquals(66, viewModel.uiState.value.overallSummary.winRatePercent)
        assertEquals(3, viewModel.uiState.value.recentLogs.size)
        assertEquals(2026, viewModel.uiState.value.selectedYear)
        assertEquals(50, viewModel.uiState.value.selectedYearSummary?.winRatePercent)
        collectionJob.cancel()
    }

    @Test
    fun onYearSelected_updatesYearSummary() = runTest {
        val repository = FakeFriendStatsRepository()
        repository.profile.value = FriendProfileLoadState(
            profile = FriendSummary(
                userId = "friend-1",
                nickname = "Moko",
                favoriteTeam = BaseballTeam.LgTwins,
                bio = "",
                photoUrl = ""
            )
        )
        repository.logsState.value = FriendLogsLoadState(
            logs = listOf(
                logEntry("1", "2026-07-12", BaseballTeam.DoosanBears, BaseballGameResult.Win),
                logEntry("2", "2025-07-06", BaseballTeam.SsgLanders, BaseballGameResult.Loss)
            )
        )
        val viewModel = FriendProfileViewModel(
            friendStatsRepository = repository,
            savedStateHandle = SavedStateHandle(
                mapOf(FRIEND_USER_ID_NAV_ARG to "friend-1")
            )
        )
        val collectionJob: Job = backgroundScope.launch {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        viewModel.onYearSelected(2025)
        advanceUntilIdle()

        assertEquals(2025, viewModel.uiState.value.selectedYear)
        assertEquals(0, viewModel.uiState.value.selectedYearSummary?.winRatePercent)
        collectionJob.cancel()
    }

    @Test
    fun uiState_marksFriendUnavailableWhenProfileMissing() = runTest {
        val repository = FakeFriendStatsRepository()
        val viewModel = FriendProfileViewModel(
            friendStatsRepository = repository,
            savedStateHandle = SavedStateHandle(
                mapOf(FRIEND_USER_ID_NAV_ARG to "friend-1")
            )
        )
        val collectionJob: Job = backgroundScope.launch {
            viewModel.uiState.collect {}
        }

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isFriendUnavailable)
        assertTrue(viewModel.uiState.value.profile == null)
        collectionJob.cancel()
    }

    @Test
    fun uiState_exposesLogsPermissionError() = runTest {
        val repository = FakeFriendStatsRepository()
        repository.profile.value = FriendProfileLoadState(
            profile = FriendSummary(
                userId = "friend-1",
                nickname = "Moko",
                favoriteTeam = BaseballTeam.LgTwins,
                bio = "",
                photoUrl = ""
            )
        )
        repository.logsState.value = FriendLogsLoadState(
            errorMessage = "This friend's game logs are blocked by Firestore rules."
        )
        val viewModel = FriendProfileViewModel(
            friendStatsRepository = repository,
            savedStateHandle = SavedStateHandle(
                mapOf(FRIEND_USER_ID_NAV_ARG to "friend-1")
            )
        )
        val collectionJob: Job = backgroundScope.launch {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        assertEquals(
            "This friend's game logs are blocked by Firestore rules.",
            viewModel.uiState.value.errorMessage
        )
        collectionJob.cancel()
    }

    private class FakeFriendStatsRepository : FriendStatsRepository {
        val profile = MutableStateFlow(FriendProfileLoadState())
        val logsState = MutableStateFlow(FriendLogsLoadState())

        override fun observeFriendProfile(friendUserId: String): Flow<FriendProfileLoadState> = profile

        override fun observeFriendLogs(friendUserId: String): Flow<FriendLogsLoadState> = logsState
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
