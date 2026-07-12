package com.mokostudio.baselog.feature.log

import com.mokostudio.baselog.core.user.BaseballTeam
import com.mokostudio.baselog.core.user.UserProfile
import com.mokostudio.baselog.core.user.UserProfileDraft
import com.mokostudio.baselog.core.user.UserProfileRepository
import com.mokostudio.baselog.testutil.MainDispatcherRule
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LogEditorViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun init_prefillsFavoriteTeamFromUserProfile() = runTest {
        val repository = FakeUserProfileRepository().apply {
            profile.value = UserProfile(
                nickname = "Moko",
                favoriteTeam = BaseballTeam.LgTwins,
                bio = "",
                email = "moko@example.com",
                photoUrl = ""
            )
        }

        val viewModel = LogEditorViewModel(
            baseballLogRepository = FakeBaseballLogRepository(),
            userProfileRepository = repository,
            currentDateProvider = { LocalDate.parse("2026-07-12") }
        )
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(BaseballTeam.LgTwins, viewModel.uiState.value.favoriteTeam)
        assertEquals(LocalDate.parse("2026-07-12"), viewModel.uiState.value.attendedDate)
    }

    @Test
    fun saveLog_emitsSavedEventAndSavesDraft() = runTest {
        val logRepository = FakeBaseballLogRepository()
        val viewModel = LogEditorViewModel(
            baseballLogRepository = logRepository,
            userProfileRepository = FakeUserProfileRepository(),
            currentDateProvider = { LocalDate.parse("2026-07-12") }
        )
        advanceUntilIdle()
        val eventDeferred = async { viewModel.events.first() }

        viewModel.onFavoriteTeamSelected(BaseballTeam.LgTwins)
        viewModel.onResultSelected(BaseballGameResult.Win)
        viewModel.saveLog()
        advanceUntilIdle()

        assertEquals(
            BaseballLogDraft(
                attendedDate = LocalDate.parse("2026-07-12"),
                team = BaseballTeam.LgTwins,
                result = BaseballGameResult.Win
            ),
            logRepository.savedDraft
        )
        assertEquals(LogEditorEvent.Saved, eventDeferred.await())
    }

    @Test
    fun saveLog_requiresResult() = runTest {
        val viewModel = LogEditorViewModel(
            baseballLogRepository = FakeBaseballLogRepository(),
            userProfileRepository = FakeUserProfileRepository(),
            currentDateProvider = { LocalDate.parse("2026-07-12") }
        )
        advanceUntilIdle()

        viewModel.onFavoriteTeamSelected(BaseballTeam.LgTwins)
        viewModel.saveLog()

        assertEquals(
            "Select a result before saving this game.",
            viewModel.uiState.value.errorMessage
        )
    }

    @Test
    fun submitEnabled_requiresTeamAndResult() = runTest {
        val viewModel = LogEditorViewModel(
            baseballLogRepository = FakeBaseballLogRepository(),
            userProfileRepository = FakeUserProfileRepository(),
            currentDateProvider = { LocalDate.parse("2026-07-12") }
        )
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSubmitEnabled)

        viewModel.onFavoriteTeamSelected(BaseballTeam.LgTwins)
        assertFalse(viewModel.uiState.value.isSubmitEnabled)

        viewModel.onResultSelected(BaseballGameResult.Win)
        assertTrue(viewModel.uiState.value.isSubmitEnabled)
    }

    private class FakeBaseballLogRepository(
        private val saveResult: Result<Unit> = Result.success(Unit)
    ) : BaseballLogRepository {
        var savedDraft: BaseballLogDraft? = null

        override fun observeLogs(): Flow<List<BaseballLogEntry>> = MutableStateFlow(emptyList())

        override suspend fun saveLog(log: BaseballLogDraft): Result<Unit> {
            savedDraft = log
            return saveResult
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
}
