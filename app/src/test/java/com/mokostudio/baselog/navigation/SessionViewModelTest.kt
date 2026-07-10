package com.mokostudio.baselog.navigation

import com.mokostudio.baselog.core.auth.AuthRepository
import com.mokostudio.baselog.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun isAuthenticated_reflectsRepositoryUpdates() = runTest {
        val repository = FakeAuthRepository()
        val viewModel = SessionViewModel(authRepository = repository)
        val collectionJob: Job = backgroundScope.launch {
            viewModel.isAuthenticated.collect {}
        }

        advanceUntilIdle()

        assertFalse(viewModel.isAuthenticated.value)

        repository.authenticated.value = true
        advanceUntilIdle()

        assertTrue(viewModel.isAuthenticated.value)
        collectionJob.cancel()
    }

    private class FakeAuthRepository : AuthRepository {
        val authenticated = MutableStateFlow(false)

        override fun observeAuthenticated(): Flow<Boolean> = authenticated

        override suspend fun signInWithGoogleIdToken(idToken: String): Result<Unit> {
            return Result.success(Unit)
        }

        override suspend fun signOut() = Unit
    }
}
