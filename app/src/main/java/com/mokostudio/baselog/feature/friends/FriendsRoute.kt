package com.mokostudio.baselog.feature.friends

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mokostudio.baselog.R
import com.mokostudio.baselog.core.user.BaseballTeam
import com.mokostudio.baselog.ui.theme.BaseLogTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsRoute(
    onBackClick: () -> Unit,
    onFriendClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FriendsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.friends_title)) },
                navigationIcon = {
                    TextButton(onClick = onBackClick) {
                        Text(text = stringResource(id = R.string.profile_edit_back))
                    }
                }
            )
        }
    ) { innerPadding ->
        FriendsScreen(
            innerPadding = innerPadding,
            uiState = uiState,
            onSearchQueryChanged = viewModel::onSearchQueryChanged,
            onSearchClick = viewModel::searchUsers,
            onSendRequestClick = viewModel::sendFriendRequest,
            onAcceptClick = viewModel::acceptRequest,
            onRejectClick = viewModel::rejectRequest,
            onRemoveFriendClick = viewModel::removeFriend,
            onFriendClick = onFriendClick,
            onLeaderboardMetricSelected = viewModel::onLeaderboardMetricSelected,
            onLeaderboardYearSelected = viewModel::onLeaderboardYearSelected,
            onErrorDismissed = viewModel::clearErrorMessage
        )
    }
}

@Composable
internal fun FriendsScreen(
    innerPadding: PaddingValues,
    uiState: FriendsUiState,
    onSearchQueryChanged: (String) -> Unit,
    onSearchClick: () -> Unit,
    onSendRequestClick: (FriendSummary) -> Unit,
    onAcceptClick: (String) -> Unit,
    onRejectClick: (String) -> Unit,
    onRemoveFriendClick: (String) -> Unit,
    onFriendClick: (String) -> Unit,
    onLeaderboardMetricSelected: (LeaderboardMetric) -> Unit,
    onLeaderboardYearSelected: (Int?) -> Unit,
    onErrorDismissed: () -> Unit
) {
    var pendingRemoveFriend by remember { mutableStateOf<FriendSummary?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
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
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.friends_search_label),
                            style = MaterialTheme.typography.titleMedium
                        )
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = onSearchQueryChanged,
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text(text = stringResource(id = R.string.friends_search_label))
                            },
                            placeholder = {
                                Text(text = stringResource(id = R.string.friends_search_placeholder))
                            },
                            singleLine = true,
                            enabled = !uiState.isSearching
                        )
                        Button(
                            onClick = onSearchClick,
                            enabled = !uiState.isSearching,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (uiState.isSearching) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(text = stringResource(id = R.string.friends_search_action))
                            }
                        }
                        when {
                            uiState.isSearchIdle -> {
                                Text(
                                    text = stringResource(id = R.string.friends_search_idle),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            uiState.searchResults.isEmpty() -> {
                                Text(
                                    text = stringResource(id = R.string.friends_search_empty),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            else -> {
                                uiState.searchResults.forEach { result ->
                                    FriendSearchCard(
                                        result = result,
                                        isProcessing = result.user.userId in uiState.actionInFlightIds,
                                        onSendRequestClick = onSendRequestClick
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                SectionHeading(title = stringResource(id = R.string.friends_incoming_title))
            }

            if (uiState.incomingRequests.isEmpty()) {
                item {
                    EmptyCard(text = stringResource(id = R.string.friends_incoming_empty))
                }
            } else {
                items(uiState.incomingRequests, key = { it.requestId }) { request ->
                    IncomingRequestCard(
                        request = request,
                        isProcessing = request.requestId in uiState.actionInFlightIds,
                        onAcceptClick = onAcceptClick,
                        onRejectClick = onRejectClick
                    )
                }
            }

            item {
                SectionHeading(title = stringResource(id = R.string.friends_list_title))
            }

            if (uiState.friends.isEmpty()) {
                item {
                    EmptyCard(text = stringResource(id = R.string.friends_list_empty))
                }
            } else {
                items(uiState.friends, key = { it.userId }) { friend ->
                    FriendCard(
                        friend = friend,
                        isProcessing = friend.userId in uiState.actionInFlightIds,
                        onViewProfileClick = {
                            onFriendClick(friend.userId)
                        },
                        onRemoveClick = {
                            pendingRemoveFriend = friend
                        }
                    )
                }
            }

            item {
                SectionHeading(title = stringResource(id = R.string.friends_leaderboard_title))
            }

            item {
                FriendLeaderboardCard(
                    leaderboard = uiState.leaderboard,
                    onMetricSelected = onLeaderboardMetricSelected,
                    onYearSelected = onLeaderboardYearSelected
                )
            }

            uiState.errorMessage?.let { message ->
                item {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    pendingRemoveFriend?.let { friend ->
        AlertDialog(
            onDismissRequest = {
                pendingRemoveFriend = null
                onErrorDismissed()
            },
            title = {
                Text(text = stringResource(id = R.string.friends_remove_confirm_title))
            },
            text = {
                Text(
                    text = stringResource(
                        id = R.string.friends_remove_confirm_body,
                        friend.nickname
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRemoveFriendClick(friend.userId)
                        pendingRemoveFriend = null
                    }
                ) {
                    Text(text = stringResource(id = R.string.friends_remove_confirm_action))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingRemoveFriend = null
                        onErrorDismissed()
                    }
                ) {
                    Text(text = stringResource(id = android.R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun FriendLeaderboardCard(
    leaderboard: FriendLeaderboardUiState,
    onMetricSelected: (LeaderboardMetric) -> Unit,
    onYearSelected: (Int?) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(id = R.string.friends_leaderboard_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LeaderboardMetric.entries.forEach { metric ->
                    FilterChipButton(
                        label = metric.label(),
                        isSelected = leaderboard.selectedMetric == metric,
                        onClick = { onMetricSelected(metric) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChipButton(
                    label = stringResource(id = R.string.logbook_filter_all),
                    isSelected = leaderboard.selectedYear == null,
                    onClick = { onYearSelected(null) },
                    modifier = Modifier.weight(1f)
                )
                leaderboard.availableYears.take(3).forEach { year ->
                    FilterChipButton(
                        label = year.toString(),
                        isSelected = leaderboard.selectedYear == year,
                        onClick = { onYearSelected(year) },
                        modifier = Modifier.weight(1f)
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
            if (leaderboard.entries.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.friends_leaderboard_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                leaderboard.entries.forEach { entry ->
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
                                        stringResource(
                                            id = R.string.friends_leaderboard_me,
                                            entry.nickname
                                        )
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
            }
        }
    }
}

@Composable
private fun FilterChipButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isSelected) {
        Button(
            onClick = onClick,
            modifier = modifier
        ) {
            Text(text = label)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier
        ) {
            Text(text = label)
        }
    }
}

@Composable
private fun SectionHeading(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
private fun EmptyCard(text: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = text,
            modifier = Modifier.padding(20.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FriendSearchCard(
    result: FriendSearchResult,
    isProcessing: Boolean,
    onSendRequestClick: (FriendSummary) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = result.user.nickname, style = MaterialTheme.typography.titleMedium)
            Text(
                text = result.user.favoriteTeam.displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            if (result.user.bio.isNotBlank()) {
                Text(
                    text = result.user.bio,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            when {
                result.isFriend -> {
                    OutlinedButton(onClick = {}, enabled = false) {
                        Text(text = stringResource(id = R.string.friends_already_friends))
                    }
                }

                result.isRequestPending -> {
                    OutlinedButton(onClick = {}, enabled = false) {
                        Text(text = stringResource(id = R.string.friends_request_sent))
                    }
                }

                else -> {
                    Button(
                        onClick = { onSendRequestClick(result.user) },
                        enabled = !isProcessing
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(text = stringResource(id = R.string.friends_send_request))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IncomingRequestCard(
    request: FriendRequest,
    isProcessing: Boolean,
    onAcceptClick: (String) -> Unit,
    onRejectClick: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = request.requester.nickname, style = MaterialTheme.typography.titleMedium)
            Text(
                text = request.requester.favoriteTeam.displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            if (request.requester.bio.isNotBlank()) {
                Text(
                    text = request.requester.bio,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onAcceptClick(request.requestId) },
                    enabled = !isProcessing,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = stringResource(id = R.string.friends_accept))
                }
                OutlinedButton(
                    onClick = { onRejectClick(request.requestId) },
                    enabled = !isProcessing,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = stringResource(id = R.string.friends_reject))
                }
            }
        }
    }
}

@Composable
private fun FriendCard(
    friend: FriendSummary,
    isProcessing: Boolean,
    onViewProfileClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = friend.nickname, style = MaterialTheme.typography.titleMedium)
            Text(
                text = friend.favoriteTeam.displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            if (friend.bio.isNotBlank()) {
                Text(
                    text = friend.bio,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(
                onClick = onViewProfileClick,
                enabled = !isProcessing,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(id = R.string.friends_view_profile))
            }
            OutlinedButton(
                onClick = onRemoveClick,
                enabled = !isProcessing,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(id = R.string.friends_remove))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FriendsScreenPreview() {
    BaseLogTheme {
        FriendsScreen(
            innerPadding = PaddingValues(),
            uiState = FriendsUiState(
                searchQuery = "Mo",
                searchResults = listOf(
                    FriendSearchResult(
                        user = FriendSummary(
                            userId = "u1",
                            nickname = "Moko",
                            favoriteTeam = BaseballTeam.LgTwins,
                            bio = "Weekday baseball regular.",
                            photoUrl = ""
                        ),
                        isFriend = false,
                        isRequestPending = false
                    )
                ),
                incomingRequests = listOf(
                    FriendRequest(
                        requestId = "r1",
                        requester = FriendSummary(
                            userId = "u2",
                            nickname = "Jin",
                            favoriteTeam = BaseballTeam.DoosanBears,
                            bio = "Always at Jamsil.",
                            photoUrl = ""
                        )
                    )
                ),
                friends = listOf(
                    FriendSummary(
                        userId = "u3",
                        nickname = "BaseFan",
                        favoriteTeam = BaseballTeam.SsgLanders,
                        bio = "",
                        photoUrl = ""
                    )
                )
            ),
            onSearchQueryChanged = {},
            onSearchClick = {},
            onSendRequestClick = {},
            onAcceptClick = {},
            onRejectClick = {},
            onRemoveFriendClick = {},
            onFriendClick = {},
            onLeaderboardMetricSelected = {},
            onLeaderboardYearSelected = {},
            onErrorDismissed = {}
        )
    }
}

@Composable
private fun LeaderboardMetric.label(): String = when (this) {
    LeaderboardMetric.WinRate -> stringResource(id = R.string.friends_leaderboard_metric_win_rate)
    LeaderboardMetric.TotalGames -> stringResource(id = R.string.friends_leaderboard_metric_total_games)
    LeaderboardMetric.Wins -> stringResource(id = R.string.friends_leaderboard_metric_wins)
}
