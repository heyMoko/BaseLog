package com.mokostudio.baselog.feature.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
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
    private val userProfileRepository: UserProfileRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val logId: String? = savedStateHandle[LOG_ID_NAV_ARG]

    private val _uiState = MutableStateFlow(
        LogEditorUiState(
            mode = if (logId == null) LogEditorMode.Create else LogEditorMode.Edit,
            attendedDate = LocalDate.now()
        )
    )
    val uiState: StateFlow<LogEditorUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<LogEditorEvent>()
    val events: SharedFlow<LogEditorEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            val profile = userProfileRepository.observeCurrentUserProfile().first()
            val existingLog = logId?.let { baseballLogRepository.observeLog(it).first() }
            _uiState.update { state ->
                state.copy(
                    favoriteTeam = profile?.favoriteTeam,
                    attendedDate = existingLog?.attendedDate ?: state.attendedDate,
                    opponentTeam = existingLog?.opponentTeam
                        ?.takeUnless { it == profile?.favoriteTeam }
                        ?: state.opponentTeam?.takeUnless { it == profile?.favoriteTeam },
                    result = existingLog?.result ?: state.result,
                    isLoading = false
                )
            }

            if (logId != null && existingLog == null) {
                _uiState.update {
                    it.copy(
                        errorMessage = LOG_EDITOR_ERROR_NOT_FOUND,
                        isMissingLog = true
                    )
                }
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

        if (state.isMissingLog) {
            return
        }

        when {
            opponentTeam == null -> {
                _uiState.update {
                    it.copy(
                        errorMessage = if (state.errorMessage == LOG_EDITOR_ERROR_SAME_TEAM) {
                            LOG_EDITOR_ERROR_SAME_TEAM
                        } else {
                            LOG_EDITOR_ERROR_OPPONENT
                        }
                    )
                }
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

            val draft = BaseballLogDraft(
                attendedDate = state.attendedDate,
                opponentTeam = opponentTeam,
                result = result
            )
            val saveResult = if (logId == null) {
                baseballLogRepository.createLog(draft)
            } else {
                baseballLogRepository.updateLog(logId = logId, log = draft)
            }

            saveResult.onSuccess {
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

    fun deleteLog() {
        val currentLogId = logId ?: return
        val state = _uiState.value
        if (state.isMissingLog) {
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isDeleting = true,
                    errorMessage = null
                )
            }

            baseballLogRepository.deleteLog(currentLogId)
                .onSuccess {
                    _uiState.update { it.copy(isDeleting = false) }
                    _events.emit(LogEditorEvent.Deleted)
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            errorMessage = throwable.message ?: LOG_EDITOR_ERROR_DELETE_UNKNOWN
                        )
                    }
                }
        }
    }
}

data class LogEditorUiState(
    val mode: LogEditorMode,
    val attendedDate: LocalDate,
    val favoriteTeam: BaseballTeam? = null,
    val opponentTeam: BaseballTeam? = null,
    val result: BaseballGameResult? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val isMissingLog: Boolean = false,
    val errorMessage: String? = null
) {
    val isSubmitEnabled: Boolean
        get() = !isLoading && !isSaving && !isDeleting && !isMissingLog &&
            opponentTeam != null && result != null

    val isDeleteEnabled: Boolean
        get() = mode == LogEditorMode.Edit && !isLoading && !isSaving && !isDeleting && !isMissingLog
}

sealed interface LogEditorEvent {
    data object Saved : LogEditorEvent
    data object Deleted : LogEditorEvent
}

enum class LogEditorMode {
    Create,
    Edit
}

private const val LOG_EDITOR_ERROR_OPPONENT = "상대 팀을 선택해주세요."
private const val LOG_EDITOR_ERROR_SAME_TEAM = "응원팀은 상대 팀으로 선택할 수 없어요."
private const val LOG_EDITOR_ERROR_RESULT = "경기 결과를 선택해주세요."
private const val LOG_EDITOR_ERROR_UNKNOWN = "직관 기록을 저장하지 못했어요. 다시 시도해주세요."
private const val LOG_EDITOR_ERROR_DELETE_UNKNOWN = "직관 기록을 삭제하지 못했어요. 다시 시도해주세요."
private const val LOG_EDITOR_ERROR_NOT_FOUND = "직관 기록을 찾지 못했어요. 목록으로 돌아가 다시 시도해주세요."
