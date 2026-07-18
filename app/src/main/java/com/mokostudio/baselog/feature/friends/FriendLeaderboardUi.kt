package com.mokostudio.baselog.feature.friends

import com.mokostudio.baselog.feature.log.WinRateSummary
import com.mokostudio.baselog.feature.log.toKoreanRecordText

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

fun List<FriendLeaderboardEntry>.toRankedEntries(
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
            supporting = summary.toKoreanRecordText() ?: "아직 기록이 없어요",
            isCurrentUser = sortValue.entry.isCurrentUser
        )
    }
}

private data class LeaderboardSortValue(
    val entry: FriendLeaderboardEntry,
    val summary: WinRateSummary,
    val primaryValue: Double,
    val secondaryValue: Int,
    val tertiaryValue: Int
)
