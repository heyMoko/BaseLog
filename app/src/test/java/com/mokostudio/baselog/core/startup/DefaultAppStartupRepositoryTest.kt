package com.mokostudio.baselog.core.startup

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultAppStartupRepositoryTest {
    @Test
    fun returnsHomeWhenAuthenticatedAndProfileCompleted() = runBlocking {
        val repository = DefaultAppStartupRepository(
            authStateDataSource = FakeAuthStateDataSource(authenticated = true),
            userProfileRepository = FakeUserProfileRepository(profileCompleted = true)
        )

        val destination = repository.observeStartupDestination().first()

        assertEquals(StartupDestination.Home, destination)
    }

    @Test
    fun returnsLoginWhenNotAuthenticated() = runBlocking {
        val repository = DefaultAppStartupRepository(
            authStateDataSource = FakeAuthStateDataSource(authenticated = false),
            userProfileRepository = FakeUserProfileRepository(profileCompleted = true)
        )

        val destination = repository.observeStartupDestination().first()

        assertEquals(StartupDestination.Login, destination)
    }

    @Test
    fun returnsOnboardingWhenAuthenticatedAndProfileIncomplete() = runBlocking {
        val repository = DefaultAppStartupRepository(
            authStateDataSource = FakeAuthStateDataSource(authenticated = true),
            userProfileRepository = FakeUserProfileRepository(profileCompleted = false)
        )

        val destination = repository.observeStartupDestination().first()

        assertEquals(StartupDestination.Onboarding, destination)
    }

    private class FakeAuthStateDataSource(
        authenticated: Boolean
    ) : AuthStateDataSource {
        private val authenticatedState = MutableStateFlow(authenticated)

        override fun observeAuthenticated() = authenticatedState
    }

    private class FakeUserProfileRepository(
        profileCompleted: Boolean
    ) : com.mokostudio.baselog.core.user.UserProfileRepository {
        private val completedState = MutableStateFlow(profileCompleted)

        override fun observeProfileCompleted() = completedState

        override fun observeCurrentUserProfile() =
            MutableStateFlow<com.mokostudio.baselog.core.user.UserProfile?>(null)

        override suspend fun saveProfile(
            profile: com.mokostudio.baselog.core.user.UserProfileDraft
        ): Result<Unit> = Result.success(Unit)

        override suspend fun syncCurrentPublicProfile(): Result<Unit> = Result.success(Unit)
    }
}
