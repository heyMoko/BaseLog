package com.mokostudio.baselog.feature.ranking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import com.mokostudio.baselog.feature.friends.FriendLeaderboardUiState
import com.mokostudio.baselog.feature.friends.LeaderboardMetric
import com.mokostudio.baselog.feature.friends.RankedFriendLeaderboardEntry
import com.mokostudio.baselog.ui.theme.BaseLogTheme

@Composable
fun RankingRoute(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: RankingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    RankingScreen(
        contentPadding = contentPadding,
        modifier = modifier,
        uiState = uiState,
        onMetricSelected = viewModel::onMetricSelected,
        onYearSelected = viewModel::onYearSelected
    )
}

@Composable
internal fun RankingScreen(
    contentPadding: PaddingValues,
    uiState: RankingUiState,
    onMetricSelected: (LeaderboardMetric) -> Unit,
    onYearSelected: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            return@Box
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = stringResource(id = R.string.ranking_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.ranking_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        uiState.myEntry?.let { myEntry ->
                            Text(
                                text = stringResource(
                                    id = R.string.ranking_my_rank,
                                    myEntry.rank
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(
                                    id = R.string.ranking_my_entry,
                                    myEntry.nickname,
                                    myEntry.value,
                                    myEntry.supporting
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        } ?: Text(
                            text = stringResource(id = R.string.ranking_my_rank_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                LeaderboardFilterSection(
                    leaderboard = uiState.leaderboard,
                    onMetricSelected = onMetricSelected,
                    onYearSelected = onYearSelected
                )
            }

            if (uiState.leaderboard.entries.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(id = R.string.ranking_empty),
                            modifier = Modifier.padding(20.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(uiState.leaderboard.entries, key = { it.userId }) { entry ->
                    LeaderboardEntryCard(entry = entry)
                }
            }
        }
    }
}

@Composable
private fun LeaderboardFilterSection(
    leaderboard: FriendLeaderboardUiState,
    onMetricSelected: (LeaderboardMetric) -> Unit,
    onYearSelected: (Int?) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LeaderboardMetric.entries.forEach { metric ->
                    FilterChipButton(
                        label = metric.label(),
                        isSelected = leaderboard.selectedMetric == metric,
                        onClick = { onMetricSelected(metric) }
                    )
                }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChipButton(
                        label = stringResource(id = R.string.logbook_filter_all),
                        isSelected = leaderboard.selectedYear == null,
                        onClick = { onYearSelected(null) }
                    )
                }
                items(leaderboard.availableYears, key = { it }) { year ->
                    FilterChipButton(
                        label = year.toString(),
                        isSelected = leaderboard.selectedYear == year,
                        onClick = { onYearSelected(year) }
                    )
                }
            }
            leaderboard.errorMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun LeaderboardEntryCard(entry: RankedFriendLeaderboardEntry) {
    Surface(
        color = if (entry.isCurrentUser) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "#${entry.rank}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = if (entry.isCurrentUser) {
                        stringResource(id = R.string.friends_leaderboard_me, entry.nickname)
                    } else {
                        entry.nickname
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = entry.favoriteTeamName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = entry.supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = entry.value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun FilterChipButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    if (isSelected) {
        Button(onClick = onClick) {
            Text(text = label)
        }
    } else {
        OutlinedButton(onClick = onClick) {
            Text(text = label)
        }
    }
}

@Composable
private fun LeaderboardMetric.label(): String = when (this) {
    LeaderboardMetric.WinRate -> stringResource(id = R.string.friends_leaderboard_metric_win_rate)
    LeaderboardMetric.TotalGames -> stringResource(id = R.string.friends_leaderboard_metric_total_games)
    LeaderboardMetric.Wins -> stringResource(id = R.string.friends_leaderboard_metric_wins)
}

@Preview(showBackground = true)
@Composable
private fun RankingScreenPreview() {
    BaseLogTheme {
        RankingScreen(
            contentPadding = PaddingValues(),
            uiState = RankingUiState(
                leaderboard = FriendLeaderboardUiState(
                    entries = listOf(
                        RankedFriendLeaderboardEntry(
                            rank = 1,
                            userId = "u1",
                            nickname = "Moko",
                            favoriteTeamName = "LG Twins",
                            value = "75%",
                            supporting = "6W 2L 0D",
                            isCurrentUser = true
                        ),
                        RankedFriendLeaderboardEntry(
                            rank = 2,
                            userId = "u2",
                            nickname = "Jin",
                            favoriteTeamName = "Doosan Bears",
                            value = "66%",
                            supporting = "4W 2L 1D",
                            isCurrentUser = false
                        )
                    ),
                    availableYears = listOf(2026, 2025)
                ),
                myEntry = RankedFriendLeaderboardEntry(
                    rank = 1,
                    userId = "u1",
                    nickname = "Moko",
                    favoriteTeamName = "LG Twins",
                    value = "75%",
                    supporting = "6W 2L 0D",
                    isCurrentUser = true
                )
            ),
            onMetricSelected = {},
            onYearSelected = {}
        )
    }
}
