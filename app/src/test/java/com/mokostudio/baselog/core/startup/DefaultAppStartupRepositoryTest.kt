package com.mokostudio.baselog.core.startup

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.mokostudio.baselog.core.datastore.UserPreferencesDataSource
import java.nio.file.Files
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultAppStartupRepositoryTest {
    @Test
    fun returnsHomeWhenAuthenticated() = runBlocking {
        val repository = DefaultAppStartupRepository(
            userPreferencesDataSource = createUserPreferencesDataSource(onboardingCompleted = false),
            authStateDataSource = FakeAuthStateDataSource(authenticated = true)
        )

        val destination = repository.observeStartupDestination().first()

        assertEquals(StartupDestination.Home, destination)
    }

    @Test
    fun returnsLoginWhenOnboardingCompletedAndNotAuthenticated() = runBlocking {
        val repository = DefaultAppStartupRepository(
            userPreferencesDataSource = createUserPreferencesDataSource(onboardingCompleted = true),
            authStateDataSource = FakeAuthStateDataSource(authenticated = false)
        )

        val destination = repository.observeStartupDestination().first()

        assertEquals(StartupDestination.Login, destination)
    }

    @Test
    fun returnsLoginWhenOnboardingIncompleteAndNotAuthenticated() = runBlocking {
        val repository = DefaultAppStartupRepository(
            userPreferencesDataSource = createUserPreferencesDataSource(onboardingCompleted = false),
            authStateDataSource = FakeAuthStateDataSource(authenticated = false)
        )

        val destination = repository.observeStartupDestination().first()

        assertEquals(StartupDestination.Login, destination)
    }

    private suspend fun createUserPreferencesDataSource(
        onboardingCompleted: Boolean
    ): UserPreferencesDataSource {
        val tempDir = Files.createTempDirectory("baselog-startup-test")
        val dataStore = PreferenceDataStoreFactory.create {
            tempDir.resolve("prefs.preferences_pb").toFile()
        }
        return UserPreferencesDataSource(dataStore).also { dataSource ->
            dataSource.setOnboardingCompleted(onboardingCompleted)
        }
    }

    private class FakeAuthStateDataSource(
        authenticated: Boolean
    ) : AuthStateDataSource {
        private val authenticatedState = MutableStateFlow(authenticated)

        override fun observeAuthenticated() = authenticatedState
    }
}
