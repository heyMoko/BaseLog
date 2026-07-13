package com.mokostudio.baselog.feature.log

import kotlinx.coroutines.flow.Flow

interface BaseballLogRepository {
    fun observeLogs(): Flow<List<BaseballLogEntry>>

    fun observeLog(logId: String): Flow<BaseballLogEntry?>

    suspend fun createLog(log: BaseballLogDraft): Result<Unit>

    suspend fun updateLog(
        logId: String,
        log: BaseballLogDraft
    ): Result<Unit>

    suspend fun deleteLog(logId: String): Result<Unit>
}
