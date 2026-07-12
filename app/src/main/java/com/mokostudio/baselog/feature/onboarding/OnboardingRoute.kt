package com.mokostudio.baselog.feature.onboarding

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
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

@Composable
fun OnboardingRoute(
    mode: OnboardingMode = OnboardingMode.Create,
    onBackClick: (() -> Unit)? = null,
    onProfileSaved: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(mode) {
        viewModel.setMode(mode)
    }

    LaunchedEffect(viewModel, mode) {
        viewModel.events.collect { event ->
            if (event is OnboardingEvent.ProfileSaved &&
                event.mode == OnboardingMode.Edit &&
                onProfileSaved != null
            ) {
                onProfileSaved()
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (mode == OnboardingMode.Edit && onBackClick != null) {
                EditProfileTopAppBar(onBackClick = onBackClick)
            }
        }
    ) { innerPadding ->
        OnboardingScreen(
            innerPadding = innerPadding,
            uiState = uiState,
            onNicknameChanged = viewModel::onNicknameChanged,
            onTeamSelected = viewModel::onTeamSelected,
            onBioChanged = viewModel::onBioChanged,
            onSubmitClick = viewModel::saveProfile
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileTopAppBar(
    onBackClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(text = stringResource(id = R.string.profile_edit_title))
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Text(text = stringResource(id = R.string.profile_edit_back))
            }
        }
    )
}

@Composable
internal fun OnboardingScreen(
    innerPadding: PaddingValues,
    uiState: OnboardingUiState,
    onNicknameChanged: (String) -> Unit,
    onTeamSelected: (BaseballTeam) -> Unit,
    onBioChanged: (String) -> Unit,
    onSubmitClick: () -> Unit
) {
    var teamMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (uiState.mode == OnboardingMode.Edit) {
                stringResource(id = R.string.profile_edit_title)
            } else {
                stringResource(id = R.string.onboarding_title)
            },
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = if (uiState.mode == OnboardingMode.Edit) {
                stringResource(id = R.string.profile_edit_body)
            } else {
                stringResource(id = R.string.onboarding_body)
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (uiState.isLoadingProfile) {
            CircularProgressIndicator()
            return@Column
        }

        OutlinedTextField(
            value = uiState.nickname,
            onValueChange = onNicknameChanged,
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text(text = stringResource(id = R.string.onboarding_nickname_label))
            },
            placeholder = {
                Text(text = stringResource(id = R.string.onboarding_nickname_placeholder))
            },
            singleLine = true,
            enabled = !uiState.isSaving
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(id = R.string.onboarding_team_label),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedButton(
                onClick = { teamMenuExpanded = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving
            ) {
                Text(
                    text = uiState.selectedTeam?.displayName
                        ?: stringResource(id = R.string.onboarding_team_placeholder),
                    modifier = Modifier.fillMaxWidth(),
                    color = if (uiState.selectedTeam == null) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }

            DropdownMenu(
                expanded = teamMenuExpanded,
                onDismissRequest = { teamMenuExpanded = false }
            ) {
                BaseballTeam.entries.forEach { team ->
                    DropdownMenuItem(
                        text = { Text(text = team.displayName) },
                        onClick = {
                            teamMenuExpanded = false
                            onTeamSelected(team)
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = uiState.bio,
            onValueChange = onBioChanged,
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text(text = stringResource(id = R.string.onboarding_bio_label))
            },
            placeholder = {
                Text(text = stringResource(id = R.string.onboarding_bio_placeholder))
            },
            minLines = 3,
            enabled = !uiState.isSaving
        )

        uiState.errorMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        Button(
            onClick = onSubmitClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.isSubmitEnabled
        ) {
            if (uiState.isSaving) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = if (uiState.mode == OnboardingMode.Edit) {
                        stringResource(id = R.string.profile_edit_submit)
                    } else {
                        stringResource(id = R.string.onboarding_submit)
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingScreenPreview() {
    BaseLogTheme {
        OnboardingScreen(
            innerPadding = PaddingValues(),
            uiState = OnboardingUiState(
                mode = OnboardingMode.Create,
                nickname = "Moko",
                selectedTeam = BaseballTeam.LgTwins,
                bio = "Weekend ballpark visitor.",
                isLoadingProfile = false
            ),
            onNicknameChanged = {},
            onTeamSelected = {},
            onBioChanged = {},
            onSubmitClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingScreenErrorPreview() {
    BaseLogTheme {
        OnboardingScreen(
            innerPadding = PaddingValues(),
            uiState = OnboardingUiState(
                mode = OnboardingMode.Edit,
                isLoadingProfile = false,
                errorMessage = "Choose your favorite team to continue."
            ),
            onNicknameChanged = {},
            onTeamSelected = {},
            onBioChanged = {},
            onSubmitClick = {}
        )
    }
}
