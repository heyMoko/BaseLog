package com.mokostudio.baselog.feature.log

import kotlin.math.floor

object WinRateCalculator {
    fun calculate(
        logs: List<BaseballLogEntry>,
        year: Int? = null
    ): WinRateSummary {
        val filteredLogs = year?.let { targetYear ->
            logs.filter { it.attendedDate.year == targetYear }
        } ?: logs

        val wins = filteredLogs.count { it.result == BaseballGameResult.Win }
        val losses = filteredLogs.count { it.result == BaseballGameResult.Loss }
        val draws = filteredLogs.count { it.result == BaseballGameResult.Draw }
        val decidedGames = wins + losses
        val winRate = if (decidedGames == 0) null else wins.toDouble() / decidedGames.toDouble()

        return WinRateSummary(
            year = year,
            totalGames = filteredLogs.size,
            wins = wins,
            losses = losses,
            draws = draws,
            winRate = winRate,
            winRatePercent = winRate?.toWholePercentage(),
            message = winRate.toWinRateMessage()
        )
    }
}

private fun Double.toWholePercentage(): Int = floor(this * 100.0).toInt()

private fun Double?.toWinRateMessage(): String? {
    val value = this ?: return null

    return when {
        value >= 1.0 -> "You're a good luck charm!"
        value >= 0.7 -> "Odds feel pretty good today."
        value >= 0.5 -> "Your game-day instincts are solid."
        value >= 0.3 -> "Maybe today is the turnaround."
        else -> "Baseball can still flip at the last moment."
    }
}
