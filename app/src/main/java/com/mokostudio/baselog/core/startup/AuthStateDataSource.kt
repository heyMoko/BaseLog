package com.mokostudio.baselog.core.startup

import kotlinx.coroutines.flow.Flow

interface AuthStateDataSource {
    fun observeAuthenticated(): Flow<Boolean>
}
