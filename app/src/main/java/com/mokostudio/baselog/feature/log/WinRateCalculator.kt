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
        value >= 1.0 -> "당신은 승리요정!"
        value >= 0.7 -> "오늘도 이길 확률이 꽤 높겠는데요?"
        value >= 0.5 -> "나쁘지 않은 직관 감각이에요."
        value >= 0.3 -> "오늘은 이길 수 있을까.."
        else -> "그래도 야구는 끝날 때까지 모르는 법."
    }
}
