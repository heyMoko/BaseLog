package com.mokostudio.baselog.core.startup

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import com.mokostudio.baselog.core.user.UserProfileRepository
import javax.inject.Inject

class DefaultAppStartupRepository @Inject constructor(
    private val authStateDataSource: AuthStateDataSource,
    private val userProfileRepository: UserProfileRepository
) : AppStartupRepository {
    override fun observeStartupDestination(): Flow<StartupDestination> {
        return combine(
            authStateDataSource.observeAuthenticated(),
            userProfileRepository.observeProfileCompleted()
        ) { isAuthenticated, isProfileCompleted ->
            when {
                !isAuthenticated -> StartupDestination.Login
                isProfileCompleted -> StartupDestination.Home
                else -> StartupDestination.Onboarding
            }
        }
    }
}
