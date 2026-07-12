package com.mokostudio.baselog.feature.log

import kotlinx.coroutines.flow.Flow

interface BaseballLogRepository {
    fun observeLogs(): Flow<List<BaseballLogEntry>>

    suspend fun saveLog(log: BaseballLogDraft): Result<Unit>
}
