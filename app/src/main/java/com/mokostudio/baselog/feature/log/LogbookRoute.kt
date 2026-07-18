package com.mokostudio.baselog.feature.log

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import java.time.LocalDate

@Composable
fun LogbookRoute(
    contentPadding: PaddingValues,
    onAddLogClick: () -> Unit,
    onEditLogClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LogbookViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LogbookScreen(
        innerPadding = contentPadding,
        modifier = modifier,
        uiState = uiState,
        onYearSelected = viewModel::onYearSelected,
        onAddLogClick = onAddLogClick,
        onEditLogClick = onEditLogClick,
        onDeleteLogClick = viewModel::onDeleteClick,
        onDeleteDismissed = viewModel::onDeleteDismissed,
        onDeleteConfirmed = viewModel::confirmDelete
    )
}

@Composable
internal fun LogbookScreen(
    innerPadding: PaddingValues,
    uiState: LogbookUiState,
    onYearSelected: (Int?) -> Unit,
    onAddLogClick: () -> Unit,
    onEditLogClick: (String) -> Unit,
    onDeleteLogClick: (BaseballLogEntry) -> Unit,
    onDeleteDismissed: () -> Unit,
    onDeleteConfirmed: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.isLoading) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

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
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = stringResource(id = R.string.logbook_title),
                    style = MaterialTheme.typography.displaySmall,
                    color = Navy900,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            item {
                LogbookSummaryCard(
                    summary = uiState.summary,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        YearFilterChipButton(
                            label = stringResource(id = R.string.logbook_filter_all),
                            isSelected = uiState.selectedYear == null,
                            onClick = { onYearSelected(null) }
                        )
                    }
                    items(uiState.availableYears, key = { it }) { year ->
                        YearFilterChipButton(
                            label = year.toString(),
                            isSelected = uiState.selectedYear == year,
                            onClick = { onYearSelected(year) }
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = onAddLogClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(id = R.string.logbook_add_action))
                }
            }

            if (uiState.isEmpty) {
                item {
                    EmptyLogbookCard(modifier = Modifier.fillMaxWidth())
                }
            } else {
                items(uiState.logs, key = { it.id }) { log ->
                    LogbookEntryCard(
                        log = log,
                        onEditClick = { onEditLogClick(log.id) },
                        onDeleteClick = { onDeleteLogClick(log) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    uiState.pendingDeleteLog?.let { log ->
        AlertDialog(
            onDismissRequest = onDeleteDismissed,
            title = {
                Text(text = stringResource(id = R.string.logbook_delete_title))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(
                            id = R.string.logbook_delete_body,
                            log.attendedDate.toString()
                        )
                    )
                    uiState.deleteErrorMessage?.let { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onDeleteConfirmed,
                    enabled = !uiState.isDeleting
                ) {
                    if (uiState.isDeleting) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(text = stringResource(id = R.string.logbook_delete_confirm))
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDeleteDismissed,
                    enabled = !uiState.isDeleting
                ) {
                    Text(text = stringResource(id = android.R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun LogbookSummaryCard(
    summary: WinRateSummary,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LogbookIconBubble(
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.EmojiEvents,
                            contentDescription = null,
                            tint = Orange500
                        )
                    }
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.logbook_summary_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = Navy900,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = summary.message ?: stringResource(id = R.string.logbook_empty_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryMetric(
                    label = stringResource(id = R.string.friends_leaderboard_metric_win_rate),
                    value = summary.winRatePercent?.let { "$it%" }
                        ?: stringResource(id = R.string.home_dashboard_empty_value),
                    modifier = Modifier.weight(1f),
                    emphasize = true
                )
                SummaryMetric(
                    label = stringResource(id = R.string.home_dashboard_total_games),
                    value = summary.totalGames.toString(),
                    modifier = Modifier.weight(1f),
                    emphasize = false
                )
            }

            HorizontalDivider(color = BorderLight)

            Text(
                text = if (summary.hasGames) {
                    stringResource(
                        id = R.string.logbook_summary_record,
                        summary.wins,
                        summary.losses,
                        summary.draws
                    )
                } else {
                    stringResource(id = R.string.home_dashboard_empty_record)
                },
                style = MaterialTheme.typography.titleMedium,
                color = Navy900,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SummaryMetric(
    label: String,
    value: String,
    emphasize: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = White,
        border = BorderStroke(1.dp, BorderLight)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = if (emphasize) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
                color = if (emphasize) Orange500 else Navy900,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EmptyLogbookCard(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, BorderLight)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(id = R.string.logbook_empty_title),
                style = MaterialTheme.typography.titleMedium,
                color = Navy900,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(id = R.string.logbook_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
        }
    }
}

@Composable
private fun LogbookEntryCard(
    log: BaseballLogEntry,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = White,
        border = BorderStroke(1.dp, BorderLight.copy(alpha = 0.9f)),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LogbookIconBubble(
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = null,
                            tint = log.result.resultColor()
                        )
                    },
                    containerColor = log.result.resultContainerColor()
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(
                            id = R.string.logbook_opponent,
                            log.opponentTeam.displayName
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = Navy900,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = log.attendedDate.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                ResultPill(result = log.result)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onEditClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(id = R.string.logbook_edit_action))
                }
                OutlinedButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(id = R.string.logbook_delete_action))
                }
            }
        }
    }
}

@Composable
private fun YearFilterChipButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    if (isSelected) {
        Button(
            onClick = onClick,
            shape = RoundedCornerShape(999.dp)
        ) {
            Text(text = label)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            shape = RoundedCornerShape(999.dp)
        ) {
            Text(text = label)
        }
    }
}

@Composable
private fun ResultPill(result: BaseballGameResult) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = result.resultContainerColor()
    ) {
        Text(
            text = result.displayName(),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = result.resultColor(),
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun LogbookIconBubble(
    icon: @Composable () -> Unit,
    containerColor: Color = Color(0xFFFFEEE3)
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}

@Composable
private fun BaseballGameResult.resultColor(): Color = when (this) {
    BaseballGameResult.Win -> Color(0xFF16A34A)
    BaseballGameResult.Loss -> Color(0xFFDC2626)
    BaseballGameResult.Draw -> TextMuted
}

@Composable
private fun BaseballGameResult.resultContainerColor(): Color = when (this) {
    BaseballGameResult.Win -> Color(0xFFE7F7EB)
    BaseballGameResult.Loss -> Color(0xFFFBE8EA)
    BaseballGameResult.Draw -> BorderLight.copy(alpha = 0.7f)
}

@Preview(showBackground = true)
@Composable
private fun LogbookScreenPreview() {
    BaseLogTheme {
        LogbookScreen(
            innerPadding = PaddingValues(),
            uiState = LogbookUiState(
                logs = listOf(
                    BaseballLogEntry(
                        id = "1",
                        attendedDate = LocalDate.parse("2026-07-12"),
                        opponentTeam = BaseballTeam.DoosanBears,
                        result = BaseballGameResult.Win
                    )
                ),
                availableYears = listOf(2026, 2025),
                summary = WinRateSummary(
                    totalGames = 1,
                    wins = 1,
                    winRatePercent = 100,
                    message = "당신은 승리요정!"
                )
            ),
            onYearSelected = {},
            onAddLogClick = {},
            onEditLogClick = {},
            onDeleteLogClick = {},
            onDeleteDismissed = {},
            onDeleteConfirmed = {}
        )
    }
}
