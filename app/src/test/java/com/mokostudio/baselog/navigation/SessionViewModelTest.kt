package com.mokostudio.baselog.navigation

import com.mokostudio.baselog.core.startup.AppStartupRepository
import com.mokostudio.baselog.core.startup.StartupDestination
import com.mokostudio.baselog.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun startupDestination_reflectsRepositoryUpdates() = runTest {
        val repository = FakeStartupRepository()
        val viewModel = SessionViewModel(appStartupRepository = repository)
        val startupCollectionJob: Job = backgroundScope.launch {
            viewModel.startupDestination.collect {}
        }
        val authCollectionJob: Job = backgroundScope.launch {
            viewModel.isAuthenticated.collect {}
        }

        advanceUntilIdle()

        assertEquals(StartupDestination.Login, viewModel.startupDestination.value)
        assertFalse(viewModel.isAuthenticated.value)

        repository.destination.value = StartupDestination.Onboarding
        advanceUntilIdle()

        assertEquals(StartupDestination.Onboarding, viewModel.startupDestination.value)
        assertTrue(viewModel.isAuthenticated.value)
        startupCollectionJob.cancel()
        authCollectionJob.cancel()
    }

    private class FakeStartupRepository : AppStartupRepository {
        val destination = MutableStateFlow(StartupDestination.Login)

        override fun observeStartupDestination(): Flow<StartupDestination> = destination
    }
}
