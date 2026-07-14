package com.mokostudio.baselog.feature.friends

import com.mokostudio.baselog.core.user.BaseballTeam
import com.mokostudio.baselog.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FriendsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun searchUsers_mapsPendingAndFriendState() = runTest {
        val repository = FakeFriendsRepository()
        repository.friends.value = listOf(friendSummary("friend-1", "BaseFan"))
        repository.pendingOutgoingIds.value = setOf("pending-1")
        repository.searchResult = listOf(
            friendSummary("friend-1", "BaseFan"),
            friendSummary("pending-1", "PendingFan"),
            friendSummary("new-1", "NewFan")
        )
        val viewModel = FriendsViewModel(repository)
        val collectionJob = backgroundScope.launch {
            viewModel.uiState.collect {}
        }

        viewModel.onSearchQueryChanged("Ba")
        viewModel.searchUsers()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.searchResults.first().isFriend)
        assertTrue(viewModel.uiState.value.searchResults[1].isRequestPending)
        assertEquals("NewFan", viewModel.uiState.value.searchResults[2].user.nickname)
        collectionJob.cancel()
    }

    @Test
    fun acceptRequest_callsRepository() = runTest {
        val repository = FakeFriendsRepository()
        val viewModel = FriendsViewModel(repository)
        val collectionJob = backgroundScope.launch {
            viewModel.uiState.collect {}
        }

        viewModel.acceptRequest("req-1")
        advanceUntilIdle()

        assertEquals("req-1", repository.acceptedRequestId)
        collectionJob.cancel()
    }

    @Test
    fun removeFriend_callsRepository() = runTest {
        val repository = FakeFriendsRepository()
        val viewModel = FriendsViewModel(repository)
        val collectionJob = backgroundScope.launch {
            viewModel.uiState.collect {}
        }

        viewModel.removeFriend("friend-1")
        advanceUntilIdle()

        assertEquals("friend-1", repository.removedFriendId)
        collectionJob.cancel()
    }

    private class FakeFriendsRepository : FriendsRepository {
        val friends = MutableStateFlow<List<FriendSummary>>(emptyList())
        val incomingRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
        val pendingOutgoingIds = MutableStateFlow<Set<String>>(emptySet())
        var searchResult: List<FriendSummary> = emptyList()
        var acceptedRequestId: String? = null
        var removedFriendId: String? = null

        override fun observeFriends(): Flow<List<FriendSummary>> = friends

        override fun observeIncomingRequests(): Flow<List<FriendRequest>> = incomingRequests

        override fun observeOutgoingPendingRequestUserIds(): Flow<Set<String>> = pendingOutgoingIds

        override suspend fun searchUsers(query: String): Result<List<FriendSummary>> {
            return Result.success(searchResult)
        }

        override suspend fun sendFriendRequest(user: FriendSummary): Result<Unit> = Result.success(Unit)

        override suspend fun acceptFriendRequest(requestId: String): Result<Unit> {
            acceptedRequestId = requestId
            return Result.success(Unit)
        }

        override suspend fun rejectFriendRequest(requestId: String): Result<Unit> = Result.success(Unit)

        override suspend fun removeFriend(friendUserId: String): Result<Unit> {
            removedFriendId = friendUserId
            return Result.success(Unit)
        }
    }

    private fun friendSummary(userId: String, nickname: String): FriendSummary {
        return FriendSummary(
            userId = userId,
            nickname = nickname,
            favoriteTeam = BaseballTeam.LgTwins,
            bio = "",
            photoUrl = ""
        )
    }
}
