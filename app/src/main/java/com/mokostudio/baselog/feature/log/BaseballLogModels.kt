package com.mokostudio.baselog.feature.log

import com.mokostudio.baselog.core.user.BaseballTeam
import java.time.LocalDate

data class BaseballLogEntry(
    val id: String,
    val attendedDate: LocalDate,
    val team: BaseballTeam,
    val result: BaseballGameResult
)

enum class BaseballGameResult {
    Win,
    Loss,
    Draw;

    companion object
}

data class WinRateSummary(
    val year: Int? = null,
    val totalGames: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val draws: Int = 0,
    val winRate: Double? = null,
    val winRatePercent: Int? = null,
    val message: String? = null
) {
    val hasGames: Boolean
        get() = totalGames > 0

    val decidedGames: Int
        get() = wins + losses
}

data class BaseballLogDraft(
    val attendedDate: LocalDate,
    val team: BaseballTeam,
    val result: BaseballGameResult
)
