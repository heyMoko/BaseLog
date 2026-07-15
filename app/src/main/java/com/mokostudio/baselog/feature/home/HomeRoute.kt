package com.mokostudio.baselog.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mokostudio.baselog.R
import com.mokostudio.baselog.ui.theme.BaseLogTheme

@Composable
fun HomeRoute(
    contentPadding: PaddingValues,
    onViewLogsClick: () -> Unit,
    onViewRankingClick: () -> Unit,
    onAddLogClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreen(
        modifier = modifier.padding(contentPadding),
        uiState = uiState,
        onViewLogsClick = onViewLogsClick,
        onViewRankingClick = onViewRankingClick,
        onAddLogClick = onAddLogClick
    )
}

@Composable
internal fun HomeScreen(
    uiState: HomeUiState,
    onViewLogsClick: () -> Unit,
    onViewRankingClick: () -> Unit,
    onAddLogClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            return@Box
        }

        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.home_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            uiState.profile?.let { profile ->
                Text(
                    text = stringResource(id = R.string.home_welcome, profile.nickname),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(id = R.string.home_team, profile.favoriteTeamName),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.home_dashboard_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (uiState.logSummary.hasLogs) {
                        HomeMetricRow(
                            title = stringResource(id = R.string.home_dashboard_overall),
                            value = uiState.logSummary.overallWinRatePercent?.let { "$it%" }
                                ?: stringResource(id = R.string.home_dashboard_pending),
                            supporting = uiState.logSummary.overallRecord.orEmpty()
                        )
                        HomeMetricRow(
                            title = stringResource(
                                id = R.string.home_dashboard_year,
                                uiState.logSummary.currentYear
                            ),
                            value = uiState.logSummary.currentYearWinRatePercent?.let { "$it%" }
                                ?: stringResource(id = R.string.home_dashboard_pending),
                            supporting = uiState.logSummary.currentYearRecord.orEmpty()
                        )
                        HomeMetricRow(
                            title = stringResource(id = R.string.home_dashboard_total_games),
                            value = uiState.logSummary.totalGames.toString(),
                            supporting = stringResource(id = R.string.home_dashboard_total_caption)
                        )
                        uiState.logSummary.overallMessage?.let { message ->
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text(
                            text = stringResource(id = R.string.home_dashboard_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.home_recent_logs_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (uiState.logSummary.recentLogs.isEmpty()) {
                        Text(
                            text = stringResource(id = R.string.home_recent_logs_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        uiState.logSummary.recentLogs.take(2).forEach { log ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = log.attendedDate,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(
                                        id = R.string.home_recent_logs_opponent,
                                        log.opponentTeamName
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = log.resultLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = onViewLogsClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(id = R.string.home_view_logs))
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.home_friends_leaderboard_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    uiState.friendLeaderboardPreview.errorMessage?.let { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    if (!uiState.friendLeaderboardPreview.hasEntries) {
                        Text(
                            text = stringResource(id = R.string.home_friends_leaderboard_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        uiState.friendLeaderboardPreview.topEntries.forEach { entry ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = if (entry.isCurrentUser) {
                                        stringResource(
                                            id = R.string.home_friends_leaderboard_me,
                                            entry.rank,
                                            entry.nickname
                                        )
                                    } else {
                                        stringResource(
                                            id = R.string.home_friends_leaderboard_rank,
                                            entry.rank,
                                            entry.nickname
                                        )
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = entry.favoriteTeamName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = stringResource(
                                        id = R.string.home_friends_leaderboard_record,
                                        entry.winRatePercent?.let { "$it%" } ?: "-",
                                        entry.record
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        uiState.friendLeaderboardPreview.myRank?.let { rank ->
                            Text(
                                text = stringResource(
                                    id = R.string.home_friends_leaderboard_my_rank,
                                    rank
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onViewRankingClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(id = R.string.home_view_ranking))
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.home_log_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(id = R.string.home_log_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = onViewLogsClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(id = R.string.home_view_logs))
                    }
                    Button(
                        onClick = onAddLogClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(id = R.string.home_add_log))
                    }
                }
            }

            if (uiState.isProfileUnavailable) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.home_profile_unavailable_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(id = R.string.home_profile_unavailable_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeMetricRow(
    title: String,
    value: String,
    supporting: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = supporting,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    BaseLogTheme {
        HomeScreen(
            uiState = HomeUiState(
                profile = HomeProfileSummary(
                    nickname = "Moko",
                    favoriteTeamName = "LG Twins",
                    bio = "Always tracking weekday games.",
                    email = "moko@example.com"
                ),
                logSummary = HomeLogSummary(
                    totalGames = 12,
                    overallWinRatePercent = 66,
                    overallRecord = "6W 3L 3D",
                    overallMessage = "Your game-day instincts are solid.",
                    currentYear = 2026,
                    currentYearWinRatePercent = 75,
                    currentYearRecord = "3W 1L 1D",
                    recentLogs = listOf(
                        HomeRecentLog(
                            id = "1",
                            attendedDate = "2026-07-12",
                            opponentTeamName = "Doosan Bears",
                            resultLabel = "Win"
                        ),
                        HomeRecentLog(
                            id = "2",
                            attendedDate = "2026-07-06",
                            opponentTeamName = "SSG Landers",
                            resultLabel = "Draw"
                        )
                    ),
                    hasLogs = true
                ),
                friendLeaderboardPreview = HomeFriendLeaderboardPreview(
                    topEntries = listOf(
                        HomeFriendLeaderboardEntry(
                            rank = 1,
                            nickname = "Moko",
                            favoriteTeamName = "LG Twins",
                            winRatePercent = 75,
                            record = "6W 2L 0D",
                            isCurrentUser = true
                        ),
                        HomeFriendLeaderboardEntry(
                            rank = 2,
                            nickname = "Jin",
                            favoriteTeamName = "Doosan Bears",
                            winRatePercent = 66,
                            record = "4W 2L 1D",
                            isCurrentUser = false
                        )
                    ),
                    myRank = 1
                )
            ),
            onViewLogsClick = {},
            onViewRankingClick = {},
            onAddLogClick = {}
        )
    }
}
