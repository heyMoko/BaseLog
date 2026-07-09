package com.mokostudio.baselog.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mokostudio.baselog.core.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class SessionViewModel @Inject constructor(
    authRepository: AuthRepository
) : ViewModel() {
    val isAuthenticated: StateFlow<Boolean> =
        authRepository.observeAuthenticated()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = false
            )
}
