package com.mokostudio.baselog.feature.mypage

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
fun MyPageRoute(
    contentPadding: PaddingValues,
    onEditProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyPageViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MyPageScreen(
        contentPadding = contentPadding,
        modifier = modifier,
        uiState = uiState,
        onEditProfileClick = onEditProfileClick,
        onSignOutClick = viewModel::signOut
    )
}

@Composable
internal fun MyPageScreen(
    contentPadding: PaddingValues,
    uiState: MyPageUiState,
    onEditProfileClick: () -> Unit,
    onSignOutClick: () -> Unit,
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.my_page_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            uiState.profile?.let { profile ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = profile.nickname,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(
                                id = R.string.my_page_team,
                                profile.favoriteTeamName
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = profile.bio.ifBlank {
                                stringResource(id = R.string.my_page_bio_empty)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (profile.email.isNotBlank()) {
                            Text(
                                text = profile.email,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (uiState.isProfileUnavailable) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
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

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.my_page_actions_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedButton(
                        onClick = onEditProfileClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(id = R.string.profile_edit_action))
                    }
                    Button(
                        onClick = onSignOutClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(id = R.string.home_sign_out))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MyPageScreenPreview() {
    BaseLogTheme {
        MyPageScreen(
            contentPadding = PaddingValues(),
            uiState = MyPageUiState(
                profile = MyPageProfile(
                    nickname = "Moko",
                    favoriteTeamName = "LG Twins",
                    bio = "Weekday baseball regular.",
                    email = "moko@example.com"
                )
            ),
            onEditProfileClick = {},
            onSignOutClick = {}
        )
    }
}
