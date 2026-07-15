package com.mokostudio.baselog.feature.friends

import com.mokostudio.baselog.core.user.BaseballTeam
import com.mokostudio.baselog.core.user.UserProfileRepository
import com.mokostudio.baselog.feature.log.BaseballLogRepository
import com.mokostudio.baselog.feature.log.WinRateCalculator
import com.mokostudio.baselog.feature.log.WinRateSummary
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

interface FriendLeaderboardRepository {
    fun observeLeaderboard(): Flow<FriendLeaderboardLoadState>
}

class DefaultFriendLeaderboardRepository @Inject constructor(
    private val friendsRepository: FriendsRepository,
    private val friendStatsRepository: FriendStatsRepository,
    private val userProfileRepository: UserProfileRepository,
    private val baseballLogRepository: BaseballLogRepository
) : FriendLeaderboardRepository {
    override fun observeLeaderboard(): Flow<FriendLeaderboardLoadState> {
        return combine(
            userProfileRepository.observeCurrentUserProfile(),
            baseballLogRepository.observeLogs(),
            friendsRepository.observeFriends()
        ) { currentUserProfile, currentUserLogs, friends ->
            Triple(currentUserProfile, currentUserLogs, friends)
        }.flatMapLatest { (currentUserProfile, currentUserLogs, friends) ->
            if (currentUserProfile == null) {
                flowOf(
                    FriendLeaderboardLoadState(
                        errorMessage = FRIEND_LEADERBOARD_PROFILE_ERROR
                    )
                )
            } else {
                val friendLogFlows = friends.map { friend ->
                    friendStatsRepository.observeFriendLogs(friend.userId)
                        .map { state -> FriendLogsSnapshot(friend = friend, state = state) }
                }
                val combinedFriendLogs = if (friendLogFlows.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    combine(friendLogFlows) { snapshots ->
                        snapshots.toList()
                    }
                }

                combinedFriendLogs.map { friendSnapshots ->
                    val currentUserEntry = FriendLeaderboardEntry(
                        userId = CURRENT_USER_ENTRY_ID,
                        nickname = currentUserProfile.nickname,
                        favoriteTeam = currentUserProfile.favoriteTeam,
                        summary = WinRateCalculator.calculate(currentUserLogs),
                        yearlySummaries = currentUserLogs.toYearlySummaryMap(),
                        isCurrentUser = true
                    )
                    val friendEntries = friendSnapshots.map { snapshot ->
                        FriendLeaderboardEntry(
                            userId = snapshot.friend.userId,
                            nickname = snapshot.friend.nickname,
                            favoriteTeam = snapshot.friend.favoriteTeam,
                            summary = WinRateCalculator.calculate(snapshot.state.logs),
                            yearlySummaries = snapshot.state.logs.toYearlySummaryMap(),
                            isCurrentUser = false
                        )
                    }
                    FriendLeaderboardLoadState(
                        entries = listOf(currentUserEntry) + friendEntries,
                        availableYears = (currentUserLogs.map { it.attendedDate.year } +
                            friendSnapshots.flatMap { snapshot ->
                                snapshot.state.logs.map { it.attendedDate.year }
                            })
                            .distinct()
                            .sortedDescending(),
                        errorMessage = friendSnapshots
                            .firstNotNullOfOrNull { it.state.errorMessage }
                    )
                }
            }
        }.distinctUntilChanged()
    }
}

data class FriendLeaderboardLoadState(
    val entries: List<FriendLeaderboardEntry> = emptyList(),
    val availableYears: List<Int> = emptyList(),
    val errorMessage: String? = null
)

data class FriendLeaderboardEntry(
    val userId: String,
    val nickname: String,
    val favoriteTeam: BaseballTeam,
    val summary: WinRateSummary,
    val yearlySummaries: Map<Int, WinRateSummary>,
    val isCurrentUser: Boolean
)

private data class FriendLogsSnapshot(
    val friend: FriendSummary,
    val state: FriendLogsLoadState
)

private fun List<com.mokostudio.baselog.feature.log.BaseballLogEntry>.toYearlySummaryMap(): Map<Int, WinRateSummary> {
    return map { it.attendedDate.year }
        .distinct()
        .associateWith { year ->
            WinRateCalculator.calculate(logs = this, year = year)
        }
}
private const val CURRENT_USER_ENTRY_ID = "current-user"
private const val FRIEND_LEADERBOARD_PROFILE_ERROR =
    "We couldn't load your profile for the leaderboard right now."
