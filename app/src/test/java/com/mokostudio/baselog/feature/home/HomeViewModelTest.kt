package com.mokostudio.baselog.feature.home

import com.mokostudio.baselog.core.auth.AuthRepository
import com.mokostudio.baselog.core.user.BaseballTeam
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
        val viewModel = HomeViewModel(
            authRepository = repository,
            userProfileRepository = FakeUserProfileRepository()
        )

        viewModel.signOut()
        advanceUntilIdle()

        assertTrue(repository.signOutCalled)
    }

    @Test
    fun uiState_reflectsCurrentUserProfile() = runTest {
        val userProfileRepository = FakeUserProfileRepository()
        val viewModel = HomeViewModel(
            authRepository = FakeAuthRepository(),
            userProfileRepository = userProfileRepository
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

        assertEquals("Moko", viewModel.uiState.value.nickname)
        assertEquals("LG Twins", viewModel.uiState.value.favoriteTeamName)
        assertTrue(viewModel.uiState.value.hasProfile)
        collectionJob.cancel()
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

    private class FakeUserProfileRepository : UserProfileRepository {
        val profile = MutableStateFlow<UserProfile?>(null)

        override fun observeProfileCompleted(): Flow<Boolean> = MutableStateFlow(false)

        override fun observeCurrentUserProfile(): Flow<UserProfile?> = profile

        override suspend fun saveProfile(profile: UserProfileDraft): Result<Unit> {
            return Result.success(Unit)
        }
    }
}
