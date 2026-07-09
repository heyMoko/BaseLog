package com.mokostudio.baselog.core.auth

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun observeAuthenticated(): Flow<Boolean>

    suspend fun signInForDevelopment(): Result<Unit>

    suspend fun signOut()
}
