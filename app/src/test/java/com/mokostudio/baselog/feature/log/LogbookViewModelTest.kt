package com.mokostudio.baselog.feature.log

import com.mokostudio.baselog.core.user.BaseballTeam
import com.mokostudio.baselog.testutil.MainDispatcherRule
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class LogbookViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun uiState_reflectsLogsAndSummary() = runTest {
        val repository = FakeBaseballLogRepository()
        val viewModel = LogbookViewModel(baseballLogRepository = repository)
        val collectionJob = backgroundScope.launch {
            viewModel.uiState.collect {}
        }

        repository.logs.value = listOf(
            logEntry("1", "2026-07-01", BaseballGameResult.Win),
            logEntry("2", "2026-07-02", BaseballGameResult.Loss),
            logEntry("3", "2025-09-01", BaseballGameResult.Win)
        )
        advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.logs.size)
        assertEquals(listOf(2026, 2025), viewModel.uiState.value.availableYears)
        assertEquals(66, viewModel.uiState.value.summary.winRatePercent)
        collectionJob.cancel()
    }

    @Test
    fun onYearSelected_filtersLogsAndSummary() = runTest {
        val repository = FakeBaseballLogRepository()
        val viewModel = LogbookViewModel(baseballLogRepository = repository)
        val collectionJob = backgroundScope.launch {
            viewModel.uiState.collect {}
        }

        repository.logs.value = listOf(
            logEntry("1", "2026-07-01", BaseballGameResult.Win),
            logEntry("2", "2026-07-02", BaseballGameResult.Loss),
            logEntry("3", "2025-09-01", BaseballGameResult.Win)
        )
        advanceUntilIdle()

        viewModel.onYearSelected(2026)
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.logs.size)
        assertEquals(2026, viewModel.uiState.value.selectedYear)
        assertEquals(50, viewModel.uiState.value.summary.winRatePercent)
        collectionJob.cancel()
    }

    @Test
    fun isEmpty_isTrueWhenNoLogsLoaded() = runTest {
        val viewModel = LogbookViewModel(baseballLogRepository = FakeBaseballLogRepository())
        val collectionJob = backgroundScope.launch {
            viewModel.uiState.collect {}
        }

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isEmpty)
        collectionJob.cancel()
    }

    private class FakeBaseballLogRepository : BaseballLogRepository {
        val logs = MutableStateFlow<List<BaseballLogEntry>>(emptyList())

        override fun observeLogs(): Flow<List<BaseballLogEntry>> = logs

        override suspend fun saveLog(log: BaseballLogDraft): Result<Unit> = Result.success(Unit)
    }

    private fun logEntry(
        id: String,
        date: String,
        result: BaseballGameResult
    ): BaseballLogEntry {
        return BaseballLogEntry(
            id = id,
            attendedDate = LocalDate.parse(date),
            team = BaseballTeam.LgTwins,
            result = result
        )
    }
}
