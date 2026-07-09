package com.mokostudio.baselog.feature.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mokostudio.baselog.core.startup.AppStartupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class SplashViewModel @Inject constructor(
    appStartupRepository: AppStartupRepository
) : ViewModel() {
    val uiState: StateFlow<SplashUiState> =
        appStartupRepository.observeStartupDestination()
            .map { destination ->
                SplashUiState.Ready(destination)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = SplashUiState.Loading
            )
}
