package com.mokostudio.baselog.feature.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val friendsRepository: FriendsRepository
) : ViewModel() {
    private val searchQuery = MutableStateFlow("")
    private val searchResults = MutableStateFlow<List<FriendSummary>>(emptyList())
    private val isSearching = MutableStateFlow(false)
    private val actionInFlightIds = MutableStateFlow<Set<String>>(emptySet())
    private val errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<FriendsUiState> =
        combine(
            combine(
                friendsRepository.observeFriends(),
                friendsRepository.observeIncomingRequests(),
                friendsRepository.observeOutgoingPendingRequestUserIds()
            ) { friends, incomingRequests, outgoingPendingIds ->
                FriendsRepositoryState(
                    friends = friends,
                    incomingRequests = incomingRequests,
                    outgoingPendingIds = outgoingPendingIds
                )
            },
            combine(
                searchQuery,
                searchResults,
                isSearching,
                actionInFlightIds,
                errorMessage
            ) { query, results, searching, actionIds, error ->
                FriendsLocalState(
                    searchQuery = query,
                    searchResults = results,
                    isSearching = searching,
                    actionInFlightIds = actionIds,
                    errorMessage = error
                )
            }
        ) { repositoryState, localState ->
            val friendIds = repositoryState.friends.map(FriendSummary::userId).toSet()
            FriendsUiState(
                searchQuery = localState.searchQuery,
                isSearching = localState.isSearching,
                searchResults = localState.searchResults.map { result ->
                    FriendSearchResult(
                        user = result,
                        isFriend = result.userId in friendIds,
                        isRequestPending = result.userId in repositoryState.outgoingPendingIds
                    )
                },
                incomingRequests = repositoryState.incomingRequests,
                friends = repositoryState.friends,
                actionInFlightIds = localState.actionInFlightIds,
                errorMessage = localState.errorMessage
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FriendsUiState(isLoading = true)
        )

    fun onSearchQueryChanged(query: String) {
        searchQuery.update { query }
        errorMessage.update { null }
        if (query.isBlank()) {
            searchResults.update { emptyList() }
        }
    }

    fun searchUsers() {
        val query = searchQuery.value.trim()
        if (query.isBlank()) {
            searchResults.update { emptyList() }
            errorMessage.update { null }
            return
        }

        viewModelScope.launch {
            isSearching.update { true }
            errorMessage.update { null }

            try {
                withTimeout(FRIENDS_SEARCH_TIMEOUT_MS) {
                    friendsRepository.searchUsers(query)
                        .onSuccess { users ->
                            searchResults.update { users }
                        }
                        .onFailure { throwable ->
                            errorMessage.update {
                                throwable.message ?: FRIENDS_SEARCH_UNKNOWN
                            }
                        }
                }
            } catch (_: Exception) {
                errorMessage.update {
                    FRIENDS_SEARCH_TIMEOUT
                }
            } finally {
                isSearching.update { false }
            }
        }
    }

    fun sendFriendRequest(user: FriendSummary) {
        runAction(user.userId) {
            friendsRepository.sendFriendRequest(user)
                .onSuccess {
                    searchResults.update { currentResults ->
                        currentResults.filterNot { result -> result.userId == user.userId } + user
                    }
                }
                .onFailure { throwable ->
                    errorMessage.update {
                        throwable.message ?: FRIENDS_REQUEST_UNKNOWN
                    }
                }
        }
    }

    fun acceptRequest(requestId: String) {
        runAction(requestId) {
            friendsRepository.acceptFriendRequest(requestId)
                .onFailure { throwable ->
                    errorMessage.update {
                        throwable.message ?: FRIENDS_ACCEPT_UNKNOWN
                    }
                }
        }
    }

    fun rejectRequest(requestId: String) {
        runAction(requestId) {
            friendsRepository.rejectFriendRequest(requestId)
                .onFailure { throwable ->
                    errorMessage.update {
                        throwable.message ?: FRIENDS_REJECT_UNKNOWN
                    }
                }
        }
    }

    fun removeFriend(friendUserId: String) {
        runAction(friendUserId) {
            friendsRepository.removeFriend(friendUserId)
                .onFailure { throwable ->
                    errorMessage.update {
                        throwable.message ?: FRIENDS_REMOVE_UNKNOWN
                    }
                }
        }
    }

    fun clearErrorMessage() {
        errorMessage.update { null }
    }

    private fun runAction(actionId: String, action: suspend () -> Unit) {
        viewModelScope.launch {
            actionInFlightIds.update { current -> current + actionId }
            errorMessage.update { null }
            action()
            actionInFlightIds.update { current -> current - actionId }
        }
    }
}

data class FriendsUiState(
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val searchResults: List<FriendSearchResult> = emptyList(),
    val incomingRequests: List<FriendRequest> = emptyList(),
    val friends: List<FriendSummary> = emptyList(),
    val actionInFlightIds: Set<String> = emptySet(),
    val errorMessage: String? = null
) {
    val isSearchIdle: Boolean
        get() = searchQuery.isBlank() && searchResults.isEmpty()
}

data class FriendSearchResult(
    val user: FriendSummary,
    val isFriend: Boolean,
    val isRequestPending: Boolean
)

private data class FriendsRepositoryState(
    val friends: List<FriendSummary>,
    val incomingRequests: List<FriendRequest>,
    val outgoingPendingIds: Set<String>
)

private data class FriendsLocalState(
    val searchQuery: String,
    val searchResults: List<FriendSummary>,
    val isSearching: Boolean,
    val actionInFlightIds: Set<String>,
    val errorMessage: String?
)

private const val FRIENDS_SEARCH_UNKNOWN = "We couldn't search for users. Try again."
private const val FRIENDS_SEARCH_TIMEOUT = "Search is taking too long. Check your connection and try again."
private const val FRIENDS_REQUEST_UNKNOWN = "We couldn't send that friend request. Try again."
private const val FRIENDS_ACCEPT_UNKNOWN = "We couldn't accept that request. Try again."
private const val FRIENDS_REJECT_UNKNOWN = "We couldn't reject that request. Try again."
private const val FRIENDS_REMOVE_UNKNOWN = "We couldn't remove that friend. Try again."
private const val FRIENDS_SEARCH_TIMEOUT_MS = 10_000L
