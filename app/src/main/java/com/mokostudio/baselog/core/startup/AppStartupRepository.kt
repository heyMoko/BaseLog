package com.mokostudio.baselog.core.startup

import kotlinx.coroutines.flow.Flow

interface AppStartupRepository {
    fun observeStartupDestination(): Flow<StartupDestination>
}
