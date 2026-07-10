package com.mokostudio.baselog.feature.home

import com.mokostudio.baselog.core.auth.AuthRepository
import com.mokostudio.baselog.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
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
        val viewModel = HomeViewModel(authRepository = repository)

        viewModel.signOut()
        advanceUntilIdle()

        assertTrue(repository.signOutCalled)
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
}
