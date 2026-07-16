package com.mokostudio.baselog.feature.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.mokostudio.baselog.ui.theme.BaseLogTheme
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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = stringResource(id = R.string.logbook_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.logbook_summary_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                    if (uiState.summary.hasGames) {
                        Text(
                            text = uiState.summary.winRatePercent?.let { "$it%" }
                                ?: stringResource(id = R.string.logbook_pending),
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(
                                id = R.string.logbook_summary_record,
                                uiState.summary.wins,
                                uiState.summary.losses,
                                uiState.summary.draws
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        uiState.summary.message?.let { message ->
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        Text(
                            text = stringResource(id = R.string.logbook_empty_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
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
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(id = R.string.logbook_add_action))
            }
        }

        if (uiState.isEmpty) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.logbook_empty_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(id = R.string.logbook_empty_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(uiState.logs, key = { it.id }) { log ->
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onEditLogClick(log.id) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = stringResource(id = R.string.logbook_edit_action))
                            }
                            OutlinedButton(
                                onClick = { onDeleteLogClick(log) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = stringResource(id = R.string.logbook_delete_action))
                            }
                        }
                    }
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
private fun YearFilterChipButton(
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
                    message = "You're a good luck charm!"
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
