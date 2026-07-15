package com.mokostudio.baselog.feature.mypage

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
class MyPageViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    userProfileRepository: UserProfileRepository
) : ViewModel() {
    val uiState: StateFlow<MyPageUiState> =
        userProfileRepository.observeCurrentUserProfile()
            .map { profile ->
                if (profile == null) {
                    MyPageUiState(
                        isLoading = false,
                        isProfileUnavailable = true
                    )
                } else {
                    MyPageUiState(
                        isLoading = false,
                        profile = MyPageProfile(
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
                initialValue = MyPageUiState(isLoading = true)
            )

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }
}

data class MyPageUiState(
    val isLoading: Boolean = false,
    val profile: MyPageProfile? = null,
    val isProfileUnavailable: Boolean = false
)

data class MyPageProfile(
    val nickname: String,
    val favoriteTeamName: String,
    val bio: String,
    val email: String
)
