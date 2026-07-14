package com.mokostudio.baselog.feature.friends

import kotlinx.coroutines.flow.Flow

interface FriendsRepository {
    fun observeFriends(): Flow<List<FriendSummary>>

    fun observeIncomingRequests(): Flow<List<FriendRequest>>

    fun observeOutgoingPendingRequestUserIds(): Flow<Set<String>>

    suspend fun searchUsers(query: String): Result<List<FriendSummary>>

    suspend fun sendFriendRequest(user: FriendSummary): Result<Unit>

    suspend fun acceptFriendRequest(requestId: String): Result<Unit>

    suspend fun rejectFriendRequest(requestId: String): Result<Unit>

    suspend fun removeFriend(friendUserId: String): Result<Unit>
}
