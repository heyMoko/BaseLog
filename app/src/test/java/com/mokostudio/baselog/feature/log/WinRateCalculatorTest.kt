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
        assertEquals("Your game-day instincts are solid.", summary.message)
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

        assertEquals("You're a good luck charm!", perfect.message)
        assertEquals("Odds feel pretty good today.", strong.message)
        assertEquals("Baseball can still flip at the last moment.", low.message)
    }

    private fun logEntry(
        id: String,
        date: String,
        result: BaseballGameResult
    ): BaseballLogEntry {
        return BaseballLogEntry(
            id = id,
            attendedDate = LocalDate.parse(date),
            team = BaseballTeam.LgTwins,
            result = result
        )
    }
}
