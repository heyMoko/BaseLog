package com.mokostudio.baselog.feature.auth.login

data class LoginUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
