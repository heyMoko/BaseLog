package com.mokostudio.baselog.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mokostudio.baselog.core.startup.AppStartupRepository
import com.mokostudio.baselog.core.startup.StartupDestination
import com.mokostudio.baselog.core.user.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SessionViewModel @Inject constructor(
    appStartupRepository: AppStartupRepository,
    userProfileRepository: UserProfileRepository
) : ViewModel() {
    val startupDestination: StateFlow<StartupDestination> =
        appStartupRepository.observeStartupDestination()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = StartupDestination.Login
            )

    val isAuthenticated: StateFlow<Boolean> =
        startupDestination
            .map { destination -> destination != StartupDestination.Login }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = false
            )

    init {
        viewModelScope.launch {
            startupDestination
                .filter { destination -> destination != StartupDestination.Login }
                .collect {
                    userProfileRepository.syncCurrentPublicProfile()
                }
        }
    }
}
