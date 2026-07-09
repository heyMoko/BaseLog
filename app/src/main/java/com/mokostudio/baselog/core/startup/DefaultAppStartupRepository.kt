package com.mokostudio.baselog.core.startup

import com.mokostudio.baselog.core.datastore.UserPreferencesDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class DefaultAppStartupRepository @Inject constructor(
    private val userPreferencesDataSource: UserPreferencesDataSource,
    private val authStateDataSource: AuthStateDataSource
) : AppStartupRepository {
    override fun observeStartupDestination(): Flow<StartupDestination> {
        return combine(
            userPreferencesDataSource.isOnboardingCompleted,
            authStateDataSource.observeAuthenticated()
        ) { onboardingCompleted, isAuthenticated ->
            when {
                isAuthenticated -> StartupDestination.Home
                onboardingCompleted -> StartupDestination.Login
                else -> StartupDestination.Login
            }
        }
    }
}
