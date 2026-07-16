package com.mokostudio.baselog.feature.log

import com.mokostudio.baselog.core.user.BaseballTeam
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WinRateCalculatorTest {
    @Test
    fun calculate_returnsEmptySummaryWhenNoLogs() {
        val summary = WinRateCalculator.calculate(emptyList())

        assertFalse(summary.hasGames)
        assertEquals(0, summary.totalGames)
        assertNull(summary.winRate)
        assertNull(summary.message)
    }

    @Test
    fun calculate_excludesDrawsFromWinRate() {
        val summary = WinRateCalculator.calculate(
            logs = listOf(
                logEntry("1", "2026-07-01", BaseballGameResult.Win),
                logEntry("2", "2026-07-02", BaseballGameResult.Loss),
                logEntry("3", "2026-07-03", BaseballGameResult.Draw)
            )
        )

        assertTrue(summary.hasGames)
        assertEquals(3, summary.totalGames)
        assertEquals(1, summary.wins)
        assertEquals(1, summary.losses)
        assertEquals(1, summary.draws)
        assertEquals(0.5, summary.winRate ?: 0.0, 0.0001)
        assertEquals(50, summary.winRatePercent)
        assertEquals("나쁘지 않은 직관 감각이에요.", summary.message)
    }

    @Test
    fun calculate_filtersLogsByYear() {
        val summary = WinRateCalculator.calculate(
            logs = listOf(
                logEntry("1", "2025-09-01", BaseballGameResult.Win),
                logEntry("2", "2026-04-01", BaseballGameResult.Loss),
                logEntry("3", "2026-05-01", BaseballGameResult.Win)
            ),
            year = 2026
        )

        assertEquals(2026, summary.year)
        assertEquals(2, summary.totalGames)
        assertEquals(1, summary.wins)
        assertEquals(1, summary.losses)
        assertEquals(0, summary.draws)
        assertEquals(0.5, summary.winRate ?: 0.0, 0.0001)
        assertEquals(50, summary.winRatePercent)
    }

    @Test
    fun calculate_returnsNullWinRateWhenOnlyDrawsExist() {
        val summary = WinRateCalculator.calculate(
            logs = listOf(
                logEntry("1", "2026-07-01", BaseballGameResult.Draw),
                logEntry("2", "2026-07-02", BaseballGameResult.Draw)
            )
        )

        assertTrue(summary.hasGames)
        assertEquals(2, summary.totalGames)
        assertEquals(0, summary.decidedGames)
        assertNull(summary.winRate)
        assertNull(summary.winRatePercent)
        assertNull(summary.message)
    }

    @Test
    fun calculate_mapsMessagesByWinRateThreshold() {
        val perfect = WinRateCalculator.calculate(
            logs = listOf(
                logEntry("1", "2026-07-01", BaseballGameResult.Win)
            )
        )
        val strong = WinRateCalculator.calculate(
            logs = listOf(
                logEntry("1", "2026-07-01", BaseballGameResult.Win),
                logEntry("2", "2026-07-02", BaseballGameResult.Win),
                logEntry("3", "2026-07-03", BaseballGameResult.Win),
                logEntry("4", "2026-07-04", BaseballGameResult.Loss)
            )
        )
        val low = WinRateCalculator.calculate(
            logs = listOf(
                logEntry("1", "2026-07-01", BaseballGameResult.Loss),
                logEntry("2", "2026-07-02", BaseballGameResult.Loss),
                logEntry("3", "2026-07-03", BaseballGameResult.Win),
                logEntry("4", "2026-07-04", BaseballGameResult.Loss)
            )
        )

        assertEquals("당신은 승리요정!", perfect.message)
        assertEquals(100, perfect.winRatePercent)
        assertEquals("오늘도 이길 확률이 꽤 높겠는데요?", strong.message)
        assertEquals(75, strong.winRatePercent)
        assertEquals("그래도 야구는 끝날 때까지 모르는 법.", low.message)
        assertEquals(25, low.winRatePercent)
    }

    @Test
    fun calculate_truncatesRecurringDecimalWinRateForDisplay() {
        val summary = WinRateCalculator.calculate(
            logs = listOf(
                logEntry("1", "2026-07-01", BaseballGameResult.Win),
                logEntry("2", "2026-07-02", BaseballGameResult.Win),
                logEntry("3", "2026-07-03", BaseballGameResult.Loss)
            )
        )

        assertEquals(66, summary.winRatePercent)
    }

    private fun logEntry(
        id: String,
        date: String,
        result: BaseballGameResult
    ): BaseballLogEntry {
        return BaseballLogEntry(
            id = id,
            attendedDate = LocalDate.parse(date),
            opponentTeam = BaseballTeam.LgTwins,
            result = result
        )
    }
}
