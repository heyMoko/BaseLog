package com.mokostudio.baselog.navigation

import com.mokostudio.baselog.core.startup.AppStartupRepository
import com.mokostudio.baselog.core.startup.StartupDestination
import com.mokostudio.baselog.core.user.UserProfile
import com.mokostudio.baselog.core.user.UserProfileDraft
import com.mokostudio.baselog.core.user.UserProfileRepository
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
        val userProfileRepository = FakeUserProfileRepository()
        val viewModel = SessionViewModel(
            appStartupRepository = repository,
            userProfileRepository = userProfileRepository
        )
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
        assertEquals(1, userProfileRepository.syncCallCount)
        startupCollectionJob.cancel()
        authCollectionJob.cancel()
    }

    @Test
    fun authenticatedDestinations_triggerPublicProfileSyncOnTransition() = runTest {
        val repository = FakeStartupRepository()
        val userProfileRepository = FakeUserProfileRepository()
        val viewModel = SessionViewModel(
            appStartupRepository = repository,
            userProfileRepository = userProfileRepository
        )
        val collectionJob: Job = backgroundScope.launch {
            viewModel.startupDestination.collect {}
        }

        advanceUntilIdle()
        assertEquals(0, userProfileRepository.syncCallCount)

        repository.destination.value = StartupDestination.Onboarding
        advanceUntilIdle()
        repository.destination.value = StartupDestination.Home
        advanceUntilIdle()

        assertEquals(2, userProfileRepository.syncCallCount)
        collectionJob.cancel()
    }

    private class FakeStartupRepository : AppStartupRepository {
        val destination = MutableStateFlow(StartupDestination.Login)

        override fun observeStartupDestination(): Flow<StartupDestination> = destination
    }

    private class FakeUserProfileRepository : UserProfileRepository {
        var syncCallCount = 0

        override fun observeProfileCompleted(): Flow<Boolean> = MutableStateFlow(false)

        override fun observeCurrentUserProfile(): Flow<UserProfile?> = MutableStateFlow(null)

        override suspend fun saveProfile(profile: UserProfileDraft): Result<Unit> = Result.success(Unit)

        override suspend fun syncCurrentPublicProfile(): Result<Unit> {
            syncCallCount += 1
            return Result.success(Unit)
        }
    }
}
