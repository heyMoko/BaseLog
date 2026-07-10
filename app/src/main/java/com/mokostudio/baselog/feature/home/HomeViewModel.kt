package com.mokostudio.baselog.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mokostudio.baselog.core.auth.AuthRepository
import com.mokostudio.baselog.core.user.UserProfile
import com.mokostudio.baselog.core.user.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    userProfileRepository: UserProfileRepository
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> =
        userProfileRepository.observeCurrentUserProfile()
            .map { profile ->
                HomeUiState(
                    nickname = profile?.nickname.orEmpty(),
                    favoriteTeamName = profile?.favoriteTeam?.displayName.orEmpty(),
                    bio = profile?.bio.orEmpty(),
                    hasProfile = profile != null
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = HomeUiState()
            )

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }
}

data class HomeUiState(
    val nickname: String = "",
    val favoriteTeamName: String = "",
    val bio: String = "",
    val hasProfile: Boolean = false
)
