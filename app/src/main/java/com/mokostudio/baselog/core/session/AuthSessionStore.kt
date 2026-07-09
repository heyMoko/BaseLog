package com.mokostudio.baselog.core.session

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class AuthSessionStore @Inject constructor() {
    private val isAuthenticated = MutableStateFlow(false)

    fun observeAuthenticated(): Flow<Boolean> = isAuthenticated.asStateFlow()

    fun updateAuthenticated(authenticated: Boolean) {
        isAuthenticated.value = authenticated
    }
}
