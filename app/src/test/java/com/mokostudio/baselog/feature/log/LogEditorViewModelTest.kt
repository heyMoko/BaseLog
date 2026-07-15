package com.mokostudio.baselog.feature.log

import androidx.lifecycle.SavedStateHandle
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
            savedStateHandle = SavedStateHandle()
        )
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(BaseballTeam.LgTwins, viewModel.uiState.value.favoriteTeam)
        assertEquals(LocalDate.now(), viewModel.uiState.value.attendedDate)
        assertEquals(LogEditorMode.Create, viewModel.uiState.value.mode)
    }

    @Test
    fun saveLog_emitsSavedEventAndSavesDraft() = runTest {
        val logRepository = FakeBaseballLogRepository()
        val viewModel = LogEditorViewModel(
            baseballLogRepository = logRepository,
            userProfileRepository = FakeUserProfileRepository(),
            savedStateHandle = SavedStateHandle()
        )
        advanceUntilIdle()
        val eventDeferred = async { viewModel.events.first() }

        viewModel.onOpponentTeamSelected(BaseballTeam.DoosanBears)
        viewModel.onResultSelected(BaseballGameResult.Win)
        viewModel.saveLog()
        advanceUntilIdle()

        assertEquals(
            BaseballLogDraft(
                attendedDate = LocalDate.now(),
                opponentTeam = BaseballTeam.DoosanBears,
                result = BaseballGameResult.Win
            ),
            logRepository.createdDraft
        )
        assertEquals(LogEditorEvent.Saved, eventDeferred.await())
    }

    @Test
    fun saveLog_requiresResult() = runTest {
        val viewModel = LogEditorViewModel(
            baseballLogRepository = FakeBaseballLogRepository(),
            userProfileRepository = FakeUserProfileRepository(),
            savedStateHandle = SavedStateHandle()
        )
        advanceUntilIdle()

        viewModel.onOpponentTeamSelected(BaseballTeam.DoosanBears)
        viewModel.saveLog()

        assertEquals(
            "Select a result before saving this game.",
            viewModel.uiState.value.errorMessage
        )
    }

    @Test
    fun onOpponentTeamSelected_rejectsFavoriteTeam() = runTest {
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
            savedStateHandle = SavedStateHandle()
        )
        advanceUntilIdle()

        viewModel.onOpponentTeamSelected(BaseballTeam.LgTwins)

        assertEquals(
            "Your team cannot be selected as the opposing team.",
            viewModel.uiState.value.errorMessage
        )
        assertEquals(null, viewModel.uiState.value.opponentTeam)
    }

    @Test
    fun saveLog_rejectsFavoriteTeamAsOpponent() = runTest {
        val logRepository = FakeBaseballLogRepository()
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
            baseballLogRepository = logRepository,
            userProfileRepository = repository,
            savedStateHandle = SavedStateHandle()
        )
        advanceUntilIdle()

        viewModel.onResultSelected(BaseballGameResult.Win)
        viewModel.onOpponentTeamSelected(BaseballTeam.LgTwins)
        viewModel.saveLog()

        assertEquals(
            "Your team cannot be selected as the opposing team.",
            viewModel.uiState.value.errorMessage
        )
        assertEquals(null, logRepository.createdDraft)
    }

    @Test
    fun submitEnabled_requiresOpponentAndResult() = runTest {
        val viewModel = LogEditorViewModel(
            baseballLogRepository = FakeBaseballLogRepository(),
            userProfileRepository = FakeUserProfileRepository(),
            savedStateHandle = SavedStateHandle()
        )
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSubmitEnabled)

        viewModel.onOpponentTeamSelected(BaseballTeam.DoosanBears)
        assertFalse(viewModel.uiState.value.isSubmitEnabled)

        viewModel.onResultSelected(BaseballGameResult.Win)
        assertTrue(viewModel.uiState.value.isSubmitEnabled)
    }

    @Test
    fun init_inEditMode_prefillsExistingLog() = runTest {
        val logRepository = FakeBaseballLogRepository().apply {
            logs.value = listOf(
                BaseballLogEntry(
                    id = "log-1",
                    attendedDate = LocalDate.parse("2026-07-10"),
                    opponentTeam = BaseballTeam.DoosanBears,
                    result = BaseballGameResult.Loss
                )
            )
        }

        val viewModel = LogEditorViewModel(
            baseballLogRepository = logRepository,
            userProfileRepository = FakeUserProfileRepository(),
            savedStateHandle = SavedStateHandle(
                mapOf(LOG_ID_NAV_ARG to "log-1")
            )
        )
        advanceUntilIdle()

        assertEquals(LogEditorMode.Edit, viewModel.uiState.value.mode)
        assertEquals(LocalDate.parse("2026-07-10"), viewModel.uiState.value.attendedDate)
        assertEquals(BaseballTeam.DoosanBears, viewModel.uiState.value.opponentTeam)
        assertEquals(BaseballGameResult.Loss, viewModel.uiState.value.result)
        assertTrue(viewModel.uiState.value.isDeleteEnabled)
    }

    @Test
    fun saveLog_inEditMode_updatesExistingDraft() = runTest {
        val logRepository = FakeBaseballLogRepository().apply {
            logs.value = listOf(
                BaseballLogEntry(
                    id = "log-1",
                    attendedDate = LocalDate.parse("2026-07-10"),
                    opponentTeam = BaseballTeam.DoosanBears,
                    result = BaseballGameResult.Loss
                )
            )
        }

        val viewModel = LogEditorViewModel(
            baseballLogRepository = logRepository,
            userProfileRepository = FakeUserProfileRepository(),
            savedStateHandle = SavedStateHandle(
                mapOf(LOG_ID_NAV_ARG to "log-1")
            )
        )
        advanceUntilIdle()

        viewModel.onResultSelected(BaseballGameResult.Win)
        viewModel.saveLog()
        advanceUntilIdle()

        assertEquals("log-1", logRepository.updatedLogId)
        assertEquals(
            BaseballLogDraft(
                attendedDate = LocalDate.parse("2026-07-10"),
                opponentTeam = BaseballTeam.DoosanBears,
                result = BaseballGameResult.Win
            ),
            logRepository.updatedDraft
        )
        assertEquals(null, logRepository.createdDraft)
    }

    @Test
    fun deleteLog_inEditMode_emitsDeletedEvent() = runTest {
        val logRepository = FakeBaseballLogRepository().apply {
            logs.value = listOf(
                BaseballLogEntry(
                    id = "log-1",
                    attendedDate = LocalDate.parse("2026-07-10"),
                    opponentTeam = BaseballTeam.DoosanBears,
                    result = BaseballGameResult.Loss
                )
            )
        }
        val viewModel = LogEditorViewModel(
            baseballLogRepository = logRepository,
            userProfileRepository = FakeUserProfileRepository(),
            savedStateHandle = SavedStateHandle(
                mapOf(LOG_ID_NAV_ARG to "log-1")
            )
        )
        advanceUntilIdle()
        val eventDeferred = async { viewModel.events.first() }

        viewModel.deleteLog()
        advanceUntilIdle()

        assertEquals("log-1", logRepository.deletedLogId)
        assertEquals(LogEditorEvent.Deleted, eventDeferred.await())
    }

    private class FakeBaseballLogRepository(
        private val createResult: Result<Unit> = Result.success(Unit),
        private val updateResult: Result<Unit> = Result.success(Unit),
        private val deleteResult: Result<Unit> = Result.success(Unit)
    ) : BaseballLogRepository {
        val logs = MutableStateFlow<List<BaseballLogEntry>>(emptyList())
        var createdDraft: BaseballLogDraft? = null
        var updatedLogId: String? = null
        var updatedDraft: BaseballLogDraft? = null
        var deletedLogId: String? = null

        override fun observeLogs(): Flow<List<BaseballLogEntry>> = logs

        override fun observeLog(logId: String): Flow<BaseballLogEntry?> {
            return MutableStateFlow(logs.value.firstOrNull { it.id == logId })
        }

        override suspend fun createLog(log: BaseballLogDraft): Result<Unit> {
            createdDraft = log
            return createResult
        }

        override suspend fun updateLog(
            logId: String,
            log: BaseballLogDraft
        ): Result<Unit> {
            updatedLogId = logId
            updatedDraft = log
            return updateResult
        }

        override suspend fun deleteLog(logId: String): Result<Unit> {
            deletedLogId = logId
            return deleteResult
        }
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
}
