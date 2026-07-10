package com.mokostudio.baselog.feature.onboarding

import com.mokostudio.baselog.core.user.BaseballTeam

data class OnboardingUiState(
    val nickname: String = "",
    val selectedTeam: BaseballTeam? = null,
    val bio: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null
) {
    val isSubmitEnabled: Boolean
        get() = !isSaving && nickname.trim().length >= 2 && selectedTeam != null
}
