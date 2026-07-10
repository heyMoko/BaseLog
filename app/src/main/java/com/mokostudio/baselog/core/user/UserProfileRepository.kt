package com.mokostudio.baselog.core.user

import kotlinx.coroutines.flow.Flow

interface UserProfileRepository {
    fun observeProfileCompleted(): Flow<Boolean>

    suspend fun saveProfile(profile: UserProfileDraft): Result<Unit>
}
