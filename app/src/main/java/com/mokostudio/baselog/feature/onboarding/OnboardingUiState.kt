package com.mokostudio.baselog.feature.onboarding

import com.mokostudio.baselog.core.user.BaseballTeam

enum class OnboardingMode {
    Create,
    Edit
}

data class OnboardingUiState(
    val mode: OnboardingMode = OnboardingMode.Create,
    val nickname: String = "",
    val selectedTeam: BaseballTeam? = null,
    val bio: String = "",
    val isLoadingProfile: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
) {
    val isSubmitEnabled: Boolean
        get() = !isLoadingProfile && !isSaving && nickname.trim().length >= 2 && selectedTeam != null
}
