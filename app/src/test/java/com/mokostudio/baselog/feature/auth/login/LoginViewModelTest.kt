package com.mokostudio.baselog.feature.auth.login

import com.mokostudio.baselog.core.auth.AuthRepository
import com.mokostudio.baselog.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun signInWithGoogleIdToken_updatesStateOnSuccess() = runTest {
        val repository = FakeAuthRepository()
        val viewModel = LoginViewModel(authRepository = repository)

        viewModel.signInWithGoogleIdToken(idToken = "token")
        advanceUntilIdle()

        assertEquals("token", repository.lastRequestedIdToken)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun signInWithGoogleIdToken_updatesStateOnFailure() = runTest {
        val repository = FakeAuthRepository(
            signInResult = Result.failure(IllegalStateException("Sign-in failed"))
        )
        val viewModel = LoginViewModel(authRepository = repository)

        viewModel.signInWithGoogleIdToken(idToken = "token")
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Sign-in failed", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun onGoogleSignInFailed_setsFallbackMessage() {
        val viewModel = LoginViewModel(authRepository = FakeAuthRepository())

        viewModel.onGoogleSignInFailed(Throwable())

        assertEquals(
            "Google sign-in failed. Try again.",
            viewModel.uiState.value.errorMessage
        )
        assertFalse(viewModel.uiState.value.isLoading)
    }

    private class FakeAuthRepository(
        private val signInResult: Result<Unit> = Result.success(Unit)
    ) : AuthRepository {
        var lastRequestedIdToken: String? = null

        override fun observeAuthenticated(): Flow<Boolean> = MutableStateFlow(false)

        override suspend fun signInWithGoogleIdToken(idToken: String): Result<Unit> {
            lastRequestedIdToken = idToken
            return signInResult
        }

        override suspend fun signOut() = Unit
    }
}
