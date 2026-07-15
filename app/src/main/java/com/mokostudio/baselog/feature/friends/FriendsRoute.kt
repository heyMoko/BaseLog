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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

@Composable
fun FriendsRoute(
    contentPadding: PaddingValues,
    onFriendClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FriendsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    FriendsScreen(
        innerPadding = contentPadding,
        modifier = modifier,
        uiState = uiState,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onSearchClick = viewModel::searchUsers,
        onSendRequestClick = viewModel::sendFriendRequest,
        onAcceptClick = viewModel::acceptRequest,
        onRejectClick = viewModel::rejectRequest,
        onRemoveFriendClick = viewModel::removeFriend,
        onFriendClick = onFriendClick,
        onErrorDismissed = viewModel::clearErrorMessage
    )
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
    onErrorDismissed: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pendingRemoveFriend by remember { mutableStateOf<FriendSummary?>(null) }

    Box(
        modifier = modifier
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
                SectionHeading(title = stringResource(id = R.string.friends_title))
            }

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
            onErrorDismissed = {}
        )
    }
}
