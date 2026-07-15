package com.mokostudio.baselog.feature.friends

import com.mokostudio.baselog.feature.log.BaseballLogEntry
import kotlinx.coroutines.flow.Flow

interface FriendStatsRepository {
    fun observeFriendProfile(friendUserId: String): Flow<FriendProfileLoadState>

    fun observeFriendLogs(friendUserId: String): Flow<FriendLogsLoadState>
}

data class FriendProfileLoadState(
    val profile: FriendSummary? = null,
    val errorMessage: String? = null
)

data class FriendLogsLoadState(
    val logs: List<BaseballLogEntry> = emptyList(),
    val errorMessage: String? = null
)
