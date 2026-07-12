package com.mokostudio.baselog.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mokostudio.baselog.core.user.BaseballTeam
import com.mokostudio.baselog.core.user.UserProfileDraft
import com.mokostudio.baselog.core.user.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<OnboardingEvent>()
    val events: SharedFlow<OnboardingEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            userProfileRepository.observeCurrentUserProfile().collect { profile ->
                _uiState.update { state ->
                    if (state.isSaving) {
                        state
                    } else {
                        state.copy(
                            nickname = profile?.nickname ?: state.nickname,
                            selectedTeam = profile?.favoriteTeam ?: state.selectedTeam,
                            bio = profile?.bio ?: state.bio,
                            isLoadingProfile = false
                        )
                    }
                }
            }
        }
    }

    fun setMode(mode: OnboardingMode) {
        _uiState.update { state ->
            if (state.mode == mode) state else state.copy(mode = mode)
        }
    }

    fun onNicknameChanged(nickname: String) {
        _uiState.update {
            it.copy(
                nickname = nickname,
                errorMessage = null
            )
        }
    }

    fun onTeamSelected(team: BaseballTeam) {
        _uiState.update {
            it.copy(
                selectedTeam = team,
                errorMessage = null
            )
        }
    }

    fun onBioChanged(bio: String) {
        _uiState.update {
            it.copy(
                bio = bio,
                errorMessage = null
            )
        }
    }

    fun saveProfile() {
        val state = _uiState.value
        val trimmedNickname = state.nickname.trim()
        val selectedTeam = state.selectedTeam

        when {
            trimmedNickname.length < 2 -> {
                _uiState.update {
                    it.copy(errorMessage = ONBOARDING_ERROR_NICKNAME)
                }
                return
            }

            selectedTeam == null -> {
                _uiState.update {
                    it.copy(errorMessage = ONBOARDING_ERROR_TEAM)
                }
                return
            }
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSaving = true,
                    errorMessage = null
                )
            }

            userProfileRepository.saveProfile(
                profile = UserProfileDraft(
                    nickname = trimmedNickname,
                    favoriteTeam = selectedTeam,
                    bio = state.bio.trim()
                )
            ).onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = throwable.message ?: ONBOARDING_ERROR_UNKNOWN
                    )
                }
            }.onSuccess {
                _uiState.update {
                    it.copy(isSaving = false)
                }
                _events.emit(OnboardingEvent.ProfileSaved(mode = state.mode))
            }
        }
    }
}

sealed interface OnboardingEvent {
    data class ProfileSaved(val mode: OnboardingMode) : OnboardingEvent
}

private const val ONBOARDING_ERROR_NICKNAME = "Nickname must be at least 2 characters."
private const val ONBOARDING_ERROR_TEAM = "Choose your favorite team to continue."
private const val ONBOARDING_ERROR_UNKNOWN = "We couldn't save your profile. Try again."
