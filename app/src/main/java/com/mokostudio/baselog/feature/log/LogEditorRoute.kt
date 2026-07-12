package com.mokostudio.baselog.feature.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mokostudio.baselog.R
import com.mokostudio.baselog.core.user.BaseballTeam
import com.mokostudio.baselog.ui.theme.BaseLogTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun LogEditorRoute(
    onBackClick: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LogEditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            if (event == LogEditorEvent.Saved) {
                onSaved()
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LogEditorTopAppBar(onBackClick = onBackClick)
        }
    ) { innerPadding ->
        LogEditorScreen(
            innerPadding = innerPadding,
            uiState = uiState,
            onDateSelected = viewModel::onDateSelected,
            onOpponentTeamSelected = viewModel::onOpponentTeamSelected,
            onResultSelected = viewModel::onResultSelected,
            onSaveClick = viewModel::saveLog
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogEditorTopAppBar(
    onBackClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(text = stringResource(id = R.string.log_editor_title))
        },
        navigationIcon = {
            TextButton(onClick = onBackClick) {
                Text(text = stringResource(id = R.string.profile_edit_back))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LogEditorScreen(
    innerPadding: PaddingValues,
    uiState: LogEditorUiState,
    onDateSelected: (LocalDate) -> Unit,
    onOpponentTeamSelected: (BaseballTeam) -> Unit,
    onResultSelected: (BaseballGameResult) -> Unit,
    onSaveClick: () -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(id = R.string.log_editor_heading),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = stringResource(id = R.string.log_editor_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (uiState.isLoading) {
            CircularProgressIndicator()
            return@Column
        }

        uiState.favoriteTeam?.let { favoriteTeam ->
            Text(
                text = stringResource(id = R.string.log_editor_my_team, favoriteTeam.displayName),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(id = R.string.log_editor_date_label),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving
            ) {
                Text(text = uiState.attendedDate.toString())
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(id = R.string.log_editor_team_label),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val selectableTeams = BaseballTeam.entries.filterNot { it == uiState.favoriteTeam }
            selectableTeams.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { team ->
                        val selected = uiState.opponentTeam == team
                        OutlinedButton(
                            onClick = { onOpponentTeamSelected(team) },
                            modifier = Modifier.weight(1f),
                            enabled = !uiState.isSaving
                        ) {
                            Text(
                                text = if (selected) "* ${team.displayName}" else team.displayName
                            )
                        }
                    }
                    if (row.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(id = R.string.log_editor_result_label),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BaseballGameResult.entries.forEach { result ->
                    val selected = uiState.result == result
                    OutlinedButton(
                        onClick = { onResultSelected(result) },
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isSaving
                    ) {
                        Text(
                            text = if (selected) "* ${result.displayName()}" else result.displayName()
                        )
                    }
                }
            }
        }

        uiState.errorMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        Button(
            onClick = onSaveClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.isSubmitEnabled
        ) {
            if (uiState.isSaving) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(text = stringResource(id = R.string.log_editor_save))
            }
        }
    }

    if (showDatePicker) {
        val initialMillis = uiState.attendedDate
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val datePickerState = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = initialMillis
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedMillis = datePickerState.selectedDateMillis
                        if (selectedMillis != null) {
                            onDateSelected(
                                Instant.ofEpochMilli(selectedMillis)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                            )
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(text = stringResource(id = android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(text = stringResource(id = android.R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LogEditorScreenPreview() {
    BaseLogTheme {
        LogEditorScreen(
            innerPadding = PaddingValues(),
            uiState = LogEditorUiState(
                attendedDate = LocalDate.parse("2026-07-12"),
                favoriteTeam = BaseballTeam.LgTwins,
                opponentTeam = BaseballTeam.DoosanBears,
                result = BaseballGameResult.Win,
                isLoading = false
            ),
            onDateSelected = {},
            onOpponentTeamSelected = {},
            onResultSelected = {},
            onSaveClick = {}
        )
    }
}

internal fun BaseballGameResult.displayName(): String = when (this) {
    BaseballGameResult.Win -> "Win"
    BaseballGameResult.Loss -> "Loss"
    BaseballGameResult.Draw -> "Draw"
}
