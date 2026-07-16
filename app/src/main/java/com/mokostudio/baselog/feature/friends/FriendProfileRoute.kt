package com.mokostudio.baselog.feature.friends

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mokostudio.baselog.R
import com.mokostudio.baselog.core.user.BaseballTeam
import com.mokostudio.baselog.feature.log.BaseballGameResult
import com.mokostudio.baselog.feature.log.BaseballLogEntry
import com.mokostudio.baselog.feature.log.WinRateSummary
import com.mokostudio.baselog.ui.theme.BaseLogTheme
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendProfileRoute(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FriendProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.friend_profile_title)) },
                navigationIcon = {
                    TextButton(onClick = onBackClick) {
                        Text(text = stringResource(id = R.string.profile_edit_back))
                    }
                }
            )
        }
    ) { innerPadding ->
        FriendProfileScreen(
            innerPadding = innerPadding,
            uiState = uiState,
            onYearSelected = viewModel::onYearSelected
        )
    }
}

@Composable
internal fun FriendProfileScreen(
    innerPadding: PaddingValues,
    uiState: FriendProfileUiState,
    onYearSelected: (Int?) -> Unit
) {
    if (uiState.isLoading) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (uiState.isFriendUnavailable || uiState.profile == null) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.friend_profile_missing_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(id = R.string.friend_profile_missing_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            uiState.errorMessage?.let { message ->
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = message,
                            modifier = Modifier.padding(20.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = uiState.profile.nickname,
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = stringResource(
                                id = R.string.friend_profile_team,
                                uiState.profile.favoriteTeam.displayName
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = uiState.profile.bio.ifBlank {
                                stringResource(id = R.string.friend_profile_bio_empty)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                StatSummaryCard(
                    title = stringResource(id = R.string.friend_profile_summary_title),
                    highlight = uiState.overallSummary.winRatePercent?.let { "$it%" },
                    primaryLabel = stringResource(id = R.string.friend_profile_overall_win_rate),
                    secondaryLabel = stringResource(id = R.string.friend_profile_total_games),
                    secondaryValue = uiState.overallSummary.totalGames.toString(),
                    recordText = overallRecordText(uiState.overallSummary),
                    message = uiState.overallSummary.message
                )
            }

            item {
                Text(
                    text = stringResource(id = R.string.friend_profile_year_filter_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FriendYearFilterChipButton(
                            label = stringResource(id = R.string.logbook_filter_all),
                            isSelected = uiState.selectedYear == null,
                            onClick = { onYearSelected(null) }
                        )
                    }
                    items(uiState.availableYears, key = { it }) { year ->
                        FriendYearFilterChipButton(
                            label = year.toString(),
                            isSelected = uiState.selectedYear == year,
                            onClick = { onYearSelected(year) }
                        )
                    }
                }
            }

            uiState.selectedYearSummary?.let { summary ->
                item {
                    StatSummaryCard(
                        title = stringResource(
                            id = R.string.friend_profile_year_win_rate,
                            uiState.selectedYear ?: summary.year ?: LocalDate.now().year
                        ),
                        highlight = summary.winRatePercent?.let { "$it%" },
                        primaryLabel = stringResource(
                            id = R.string.friend_profile_year_win_rate,
                            uiState.selectedYear ?: summary.year ?: LocalDate.now().year
                        ),
                        secondaryLabel = stringResource(id = R.string.friend_profile_total_games),
                        secondaryValue = summary.totalGames.toString(),
                        recordText = overallRecordText(summary),
                        message = summary.message
                    )
                }
            }

            item {
                Text(
                    text = stringResource(id = R.string.friend_profile_recent_logs_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (uiState.recentLogs.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(id = R.string.friend_profile_recent_logs_empty),
                            modifier = Modifier.padding(20.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(uiState.recentLogs, key = { it.id }) { log ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = log.attendedDate.toString(),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = stringResource(
                                    id = R.string.logbook_opponent,
                                    log.opponentTeam.displayName
                                ),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = log.result.displayName(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FriendYearFilterChipButton(
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
private fun StatSummaryCard(
    title: String,
    highlight: String?,
    primaryLabel: String,
    secondaryLabel: String,
    secondaryValue: String,
    recordText: String?,
    message: String?
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
            Text(
                text = highlight ?: stringResource(id = R.string.logbook_pending),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = primaryLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$secondaryLabel: $secondaryValue",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            recordText?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            message?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun overallRecordText(summary: WinRateSummary): String? {
    if (!summary.hasGames) return null

    return "${summary.wins}W ${summary.losses}L ${summary.draws}D"
}

private fun BaseballGameResult.displayName(): String = when (this) {
    BaseballGameResult.Win -> "승"
    BaseballGameResult.Loss -> "패"
    BaseballGameResult.Draw -> "무"
}

@Preview(showBackground = true)
@Composable
private fun FriendProfileScreenPreview() {
    BaseLogTheme {
        FriendProfileScreen(
            innerPadding = PaddingValues(),
            uiState = FriendProfileUiState(
                profile = FriendSummary(
                    userId = "friend-1",
                    nickname = "MokoFan",
                    favoriteTeam = BaseballTeam.LgTwins,
                    bio = "Weekday game tracker.",
                    photoUrl = ""
                ),
                availableYears = listOf(2026, 2025),
                selectedYear = 2026,
                overallSummary = WinRateSummary(
                    totalGames = 4,
                    wins = 3,
                    losses = 1,
                    winRatePercent = 75,
                    message = "Odds feel pretty good today."
                ),
                selectedYearSummary = WinRateSummary(
                    year = 2026,
                    totalGames = 3,
                    wins = 2,
                    losses = 1,
                    winRatePercent = 66,
                    message = "Your game-day instincts are solid."
                ),
                recentLogs = listOf(
                    BaseballLogEntry(
                        id = "1",
                        attendedDate = LocalDate.parse("2026-07-12"),
                        opponentTeam = BaseballTeam.DoosanBears,
                        result = BaseballGameResult.Win
                    )
                )
            ),
            onYearSelected = {}
        )
    }
}
