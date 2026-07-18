package com.mokostudio.baselog.feature.friends

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mokostudio.baselog.R
import com.mokostudio.baselog.core.user.BaseballTeam
import com.mokostudio.baselog.ui.theme.BaseLogTheme
import com.mokostudio.baselog.ui.theme.BorderLight
import com.mokostudio.baselog.ui.theme.Navy900
import com.mokostudio.baselog.ui.theme.Orange500
import com.mokostudio.baselog.ui.theme.SurfaceLight
import com.mokostudio.baselog.ui.theme.TextMuted
import com.mokostudio.baselog.ui.theme.White

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
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFFFFBF7), SurfaceLight, White)
                )
            )
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
                TopLevelTitle(title = stringResource(id = R.string.friends_title))
            }

            item {
                FriendSearchSection(
                    uiState = uiState,
                    onSearchQueryChanged = onSearchQueryChanged,
                    onSearchClick = onSearchClick,
                    onSendRequestClick = onSendRequestClick
                )
            }

            item {
                SectionHeading(
                    title = stringResource(id = R.string.friends_incoming_title),
                    icon = Icons.Outlined.Inbox
                )
            }

            if (uiState.incomingRequests.isEmpty()) {
                item {
                    EmptyCard(
                        text = stringResource(id = R.string.friends_incoming_empty),
                        icon = Icons.Outlined.Inbox
                    )
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
                SectionHeading(
                    title = stringResource(id = R.string.friends_list_title),
                    icon = Icons.Outlined.Groups
                )
            }

            if (uiState.friends.isEmpty()) {
                item {
                    EmptyCard(
                        text = stringResource(id = R.string.friends_list_empty),
                        icon = Icons.Outlined.Groups
                    )
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
                    ErrorCard(message = message)
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
private fun TopLevelTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.displaySmall,
        color = Navy900,
        fontWeight = FontWeight.ExtraBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun FriendSearchSection(
    uiState: FriendsUiState,
    onSearchQueryChanged: (String) -> Unit,
    onSearchClick: () -> Unit,
    onSendRequestClick: (FriendSummary) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FriendsIconBubble(
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.PersonAdd,
                            contentDescription = null,
                            tint = Orange500
                        )
                    }
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = stringResource(id = R.string.friends_search_label),
                    style = MaterialTheme.typography.titleLarge,
                    color = Navy900,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChanged,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null
                    )
                },
                placeholder = {
                    Text(text = stringResource(id = R.string.friends_search_placeholder))
                },
                singleLine = true,
                enabled = !uiState.isSearching,
                shape = RoundedCornerShape(18.dp)
            )

            Button(
                onClick = onSearchClick,
                enabled = !uiState.isSearching,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                if (uiState.isSearching) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(id = R.string.friends_search_action))
                }
            }

            when {
                uiState.isSearchIdle -> {
                    SearchMessage(text = stringResource(id = R.string.friends_search_idle))
                }

                uiState.searchResults.isEmpty() -> {
                    SearchMessage(text = stringResource(id = R.string.friends_search_empty))
                }

                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
}

@Composable
private fun SectionHeading(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        FriendsIconBubble(
            icon = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Orange500
                )
            },
            size = 36.dp
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = Navy900,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EmptyCard(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = White,
        border = BorderStroke(1.dp, BorderLight)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FriendsIconBubble(
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = TextMuted
                    )
                },
                containerColor = BorderLight.copy(alpha = 0.55f)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SearchMessage(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = White,
        border = BorderStroke(1.dp, BorderLight.copy(alpha = 0.8f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FriendSearchCard(
    result: FriendSearchResult,
    isProcessing: Boolean,
    onSendRequestClick: (FriendSummary) -> Unit
) {
    FriendSurface {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FriendIdentity(
                friend = result.user,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            FriendSearchAction(
                result = result,
                isProcessing = isProcessing,
                onSendRequestClick = onSendRequestClick
            )
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
    FriendSurface {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            FriendIdentity(friend = request.requester)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onAcceptClick(request.requestId) },
                    enabled = !isProcessing,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(id = R.string.friends_accept))
                }
                OutlinedButton(
                    onClick = { onRejectClick(request.requestId) },
                    enabled = !isProcessing,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
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
    FriendSurface {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            FriendIdentity(friend = friend)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onViewProfileClick,
                    enabled = !isProcessing,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(id = R.string.friends_view_profile))
                }
                OutlinedButton(
                    onClick = onRemoveClick,
                    enabled = !isProcessing,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(id = R.string.friends_remove))
                }
            }
        }
    }
}

@Composable
private fun FriendSearchAction(
    result: FriendSearchResult,
    isProcessing: Boolean,
    onSendRequestClick: (FriendSummary) -> Unit
) {
    when {
        result.isFriend -> {
            StatusPill(text = stringResource(id = R.string.friends_already_friends))
        }

        result.isRequestPending -> {
            StatusPill(text = stringResource(id = R.string.friends_request_sent))
        }

        else -> {
            Button(
                onClick = { onSendRequestClick(result.user) },
                enabled = !isProcessing,
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(id = R.string.friends_send_request))
                }
            }
        }
    }
}

@Composable
private fun FriendIdentity(
    friend: FriendSummary,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FriendAvatar(nickname = friend.nickname)
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = friend.nickname,
                style = MaterialTheme.typography.titleMedium,
                color = Navy900,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = friend.favoriteTeam.displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = Orange500,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (friend.bio.isNotBlank()) {
                Text(
                    text = friend.bio,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun FriendSurface(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = White,
        border = BorderStroke(1.dp, BorderLight.copy(alpha = 0.9f)),
        shadowElevation = 2.dp
    ) {
        Box(modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp)) {
            content()
        }
    }
}

@Composable
private fun FriendAvatar(nickname: String) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color(0xFFFFEEE3)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = nickname.trim().firstOrNull()?.uppercaseChar()?.toString().orEmpty(),
            style = MaterialTheme.typography.titleMedium,
            color = Orange500,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun StatusPill(text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = BorderLight.copy(alpha = 0.65f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = TextMuted,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FriendsIconBubble(
    icon: @Composable () -> Unit,
    size: androidx.compose.ui.unit.Dp = 44.dp,
    containerColor: Color = Color(0xFFFFEEE3)
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}

@Composable
private fun ErrorCard(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFFFFF2F0),
        border = BorderStroke(1.dp, Color(0xFFFFD6CF))
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
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
                            bio = "잠실 평일 경기 자주 가요.",
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
                            bio = "두산 홈경기 직관러",
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
