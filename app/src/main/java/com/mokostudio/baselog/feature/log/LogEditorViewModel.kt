package com.mokostudio.baselog.feature.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mokostudio.baselog.core.user.BaseballTeam
import com.mokostudio.baselog.core.user.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LogEditorViewModel @Inject constructor(
    private val baseballLogRepository: BaseballLogRepository,
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        LogEditorUiState(
            attendedDate = LocalDate.now()
        )
    )
    val uiState: StateFlow<LogEditorUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<LogEditorEvent>()
    val events: SharedFlow<LogEditorEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            val profile = userProfileRepository.observeCurrentUserProfile().first()
            _uiState.update { state ->
                state.copy(
                    favoriteTeam = profile?.favoriteTeam,
                    opponentTeam = state.opponentTeam?.takeUnless { it == profile?.favoriteTeam },
                    isLoading = false
                )
            }
        }
    }

    fun onDateSelected(date: LocalDate) {
        _uiState.update { state ->
            state.copy(
                attendedDate = date,
                errorMessage = null
            )
        }
    }

    fun onOpponentTeamSelected(team: BaseballTeam) {
        _uiState.update { state ->
            if (team == state.favoriteTeam) {
                return@update state.copy(errorMessage = LOG_EDITOR_ERROR_SAME_TEAM)
            }
            state.copy(
                opponentTeam = team,
                errorMessage = null
            )
        }
    }

    fun onResultSelected(result: BaseballGameResult) {
        _uiState.update { state ->
            state.copy(
                result = result,
                errorMessage = null
            )
        }
    }

    fun saveLog() {
        val state = _uiState.value
        val opponentTeam = state.opponentTeam
        val result = state.result

        when {
            opponentTeam == null -> {
                _uiState.update { it.copy(errorMessage = LOG_EDITOR_ERROR_OPPONENT) }
                return
            }

            opponentTeam == state.favoriteTeam -> {
                _uiState.update { it.copy(errorMessage = LOG_EDITOR_ERROR_SAME_TEAM) }
                return
            }

            result == null -> {
                _uiState.update { it.copy(errorMessage = LOG_EDITOR_ERROR_RESULT) }
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

            baseballLogRepository.saveLog(
                BaseballLogDraft(
                    attendedDate = state.attendedDate,
                    opponentTeam = opponentTeam,
                    result = result
                )
            ).onSuccess {
                _uiState.update { it.copy(isSaving = false) }
                _events.emit(LogEditorEvent.Saved)
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = throwable.message ?: LOG_EDITOR_ERROR_UNKNOWN
                    )
                }
            }
        }
    }
}

data class LogEditorUiState(
    val attendedDate: LocalDate,
    val favoriteTeam: BaseballTeam? = null,
    val opponentTeam: BaseballTeam? = null,
    val result: BaseballGameResult? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
) {
    val isSubmitEnabled: Boolean
        get() = !isLoading && !isSaving && opponentTeam != null && result != null
}

sealed interface LogEditorEvent {
    data object Saved : LogEditorEvent
}

private const val LOG_EDITOR_ERROR_OPPONENT = "Choose the opposing team before saving this game."
private const val LOG_EDITOR_ERROR_SAME_TEAM = "Your team cannot be selected as the opposing team."
private const val LOG_EDITOR_ERROR_RESULT = "Select a result before saving this game."
private const val LOG_EDITOR_ERROR_UNKNOWN = "We couldn't save this game log. Try again."
