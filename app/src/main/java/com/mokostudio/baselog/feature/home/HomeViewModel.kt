package com.mokostudio.baselog.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mokostudio.baselog.core.auth.AuthRepository
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
                if (profile == null) {
                    HomeUiState(
                        isLoading = false,
                        isProfileUnavailable = true
                    )
                } else {
                    HomeUiState(
                        isLoading = false,
                        profile = HomeProfileSummary(
                            nickname = profile.nickname,
                            favoriteTeamName = profile.favoriteTeam.displayName,
                            bio = profile.bio,
                            email = profile.email
                        )
                    )
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = HomeUiState(isLoading = true)
            )

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }
}

data class HomeUiState(
    val isLoading: Boolean = false,
    val profile: HomeProfileSummary? = null,
    val isProfileUnavailable: Boolean = false
)

data class HomeProfileSummary(
    val nickname: String,
    val favoriteTeamName: String,
    val bio: String,
    val email: String
)
