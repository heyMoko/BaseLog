package com.mokostudio.baselog.core.auth

import com.mokostudio.baselog.core.datastore.UserPreferencesDataSource
import com.mokostudio.baselog.core.session.AuthSessionStore
import javax.inject.Inject
import kotlinx.coroutines.delay

class FakeAuthRepository @Inject constructor(
    private val authSessionStore: AuthSessionStore,
    private val userPreferencesDataSource: UserPreferencesDataSource
) : AuthRepository {
    override fun observeAuthenticated() = authSessionStore.observeAuthenticated()

    override suspend fun signInForDevelopment(): Result<Unit> {
        delay(800L)
        authSessionStore.updateAuthenticated(true)
        userPreferencesDataSource.setOnboardingCompleted(completed = true)
        return Result.success(Unit)
    }

    override suspend fun signOut() {
        authSessionStore.updateAuthenticated(false)
    }
}
