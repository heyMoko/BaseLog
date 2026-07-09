package com.mokostudio.baselog.feature.splash

import com.mokostudio.baselog.core.startup.StartupDestination

sealed interface SplashUiState {
    data object Loading : SplashUiState
    data class Ready(val destination: StartupDestination) : SplashUiState
}
