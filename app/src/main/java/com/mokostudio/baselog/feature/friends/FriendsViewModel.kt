package com.mokostudio.baselog.feature.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mokostudio.baselog.core.user.UserProfileRepository
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
    private val friendsRepository: FriendsRepository,
    userProfileRepository: UserProfileRepository,
    friendLeaderboardRepository: FriendLeaderboardRepository
) : ViewModel() {
    private val searchQuery = MutableStateFlow("")
    private val searchResults = MutableStateFlow<List<FriendSummary>>(emptyList())
    private val isSearching = MutableStateFlow(false)
    private val actionInFlightIds = MutableStateFlow<Set<String>>(emptySet())
    private val errorMessage = MutableStateFlow<String?>(null)
    private val selectedLeaderboardMetric = MutableStateFlow(LeaderboardMetric.WinRate)
    private val selectedLeaderboardYear = MutableStateFlow<Int?>(null)

    init {
        viewModelScope.launch {
            userProfileRepository.syncCurrentPublicProfile()
        }
    }

    val uiState: StateFlow<FriendsUiState> =
        combine(
            combine(
                combine(
                    friendsRepository.observeFriends(),
                    friendsRepository.observeIncomingRequests(),
                    friendsRepository.observeOutgoingPendingRequestUserIds()
                ) { friends, incomingRequests, outgoingPendingIds ->
                    Triple(friends, incomingRequests, outgoingPendingIds)
                },
                combine(
                    friendLeaderboardRepository.observeLeaderboard(),
                    selectedLeaderboardMetric,
                    selectedLeaderboardYear
                ) { leaderboardState, metric, selectedYear ->
                    LeaderboardSelectionState(
                        leaderboard = leaderboardState,
                        selectedMetric = metric,
                        selectedYear = selectedYear
                    )
                }
            ) { repositoryValues, leaderboardSelection ->
                val (friends, incomingRequests, outgoingPendingIds) = repositoryValues
                FriendsRepositoryState(
                    friends = friends,
                    incomingRequests = incomingRequests,
                    outgoingPendingIds = outgoingPendingIds,
                    leaderboard = leaderboardSelection.leaderboard,
                    selectedLeaderboardMetric = leaderboardSelection.selectedMetric,
                    selectedLeaderboardYear = leaderboardSelection.selectedYear
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
            val resolvedLeaderboardYear =
                repositoryState.selectedLeaderboardYear
                    ?.takeIf { it in repositoryState.leaderboard.availableYears }
            val leaderboardEntries = repositoryState.leaderboard.entries.toRankedEntries(
                metric = repositoryState.selectedLeaderboardMetric,
                selectedYear = resolvedLeaderboardYear
            )
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
                leaderboard = FriendLeaderboardUiState(
                    entries = leaderboardEntries,
                    availableYears = repositoryState.leaderboard.availableYears,
                    selectedMetric = repositoryState.selectedLeaderboardMetric,
                    selectedYear = resolvedLeaderboardYear,
                    errorMessage = repositoryState.leaderboard.errorMessage
                ),
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

    fun onLeaderboardMetricSelected(metric: LeaderboardMetric) {
        selectedLeaderboardMetric.update { metric }
    }

    fun onLeaderboardYearSelected(year: Int?) {
        selectedLeaderboardYear.update { year }
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
    val leaderboard: FriendLeaderboardUiState = FriendLeaderboardUiState(),
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
    val outgoingPendingIds: Set<String>,
    val leaderboard: FriendLeaderboardLoadState,
    val selectedLeaderboardMetric: LeaderboardMetric,
    val selectedLeaderboardYear: Int?
)

private data class LeaderboardSelectionState(
    val leaderboard: FriendLeaderboardLoadState,
    val selectedMetric: LeaderboardMetric,
    val selectedYear: Int?
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

data class FriendLeaderboardUiState(
    val entries: List<RankedFriendLeaderboardEntry> = emptyList(),
    val availableYears: List<Int> = emptyList(),
    val selectedMetric: LeaderboardMetric = LeaderboardMetric.WinRate,
    val selectedYear: Int? = null,
    val errorMessage: String? = null
)

data class RankedFriendLeaderboardEntry(
    val rank: Int,
    val userId: String,
    val nickname: String,
    val favoriteTeamName: String,
    val value: String,
    val supporting: String,
    val isCurrentUser: Boolean
)

enum class LeaderboardMetric {
    WinRate,
    TotalGames,
    Wins
}

private fun List<FriendLeaderboardEntry>.toRankedEntries(
    metric: LeaderboardMetric,
    selectedYear: Int?
): List<RankedFriendLeaderboardEntry> {
    return map { entry ->
        val summary = selectedYear?.let(entry.yearlySummaries::get) ?: entry.summary
        LeaderboardSortValue(
            entry = entry,
            summary = summary,
            primaryValue = when (metric) {
                LeaderboardMetric.WinRate -> summary.winRate ?: -1.0
                LeaderboardMetric.TotalGames -> summary.totalGames.toDouble()
                LeaderboardMetric.Wins -> summary.wins.toDouble()
            },
            secondaryValue = summary.totalGames,
            tertiaryValue = summary.wins
        )
    }.sortedWith(
        compareByDescending<LeaderboardSortValue> { it.primaryValue }
            .thenByDescending { it.secondaryValue }
            .thenByDescending { it.tertiaryValue }
            .thenBy { it.entry.nickname.lowercase() }
    ).mapIndexed { index, sortValue ->
        val summary = sortValue.summary
        RankedFriendLeaderboardEntry(
            rank = index + 1,
            userId = sortValue.entry.userId,
            nickname = sortValue.entry.nickname,
            favoriteTeamName = sortValue.entry.favoriteTeam.displayName,
            value = when (metric) {
                LeaderboardMetric.WinRate -> summary.winRatePercent?.let { "$it%" } ?: "-"
                LeaderboardMetric.TotalGames -> summary.totalGames.toString()
                LeaderboardMetric.Wins -> summary.wins.toString()
            },
            supporting = "${summary.wins}W ${summary.losses}L ${summary.draws}D",
            isCurrentUser = sortValue.entry.isCurrentUser
        )
    }
}

private data class LeaderboardSortValue(
    val entry: FriendLeaderboardEntry,
    val summary: com.mokostudio.baselog.feature.log.WinRateSummary,
    val primaryValue: Double,
    val secondaryValue: Int,
    val tertiaryValue: Int
)
