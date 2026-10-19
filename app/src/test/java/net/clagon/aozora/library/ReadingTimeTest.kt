package net.clagon.aozora.library

import net.clagon.aozora.reader.MAX_PAGE_MILLIS
import net.clagon.aozora.reader.ReadingClock
import net.clagon.aozora.ui.other.dayLabel
import net.clagon.aozora.ui.other.minutesLabel
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ReadingTimeTest {
    private val today = LocalDate.of(2026, 10, 19) // 月曜日

    @Test
    fun lastSevenDaysFillsDaysWithoutReading() {
        val days = lastSevenDays(today, listOf(ReadingTime("2026-10-19", 5), ReadingTime("2026-10-14", 3)))
        assertEquals(LocalDate.of(2026, 10, 13), days.first().day)
        assertEquals(listOf(0L, 3, 0, 0, 0, 0, 5), days.map { it.millis })
    }

    @Test
    fun labels() {
        assertEquals(
            listOf("火", "水", "木", "金", "土", "昨", "今"),
            (6 downTo 0).map {
                dayLabel(today.minusDays(it.toLong()), today)
            },
        )
        assertEquals("4m", minutesLabel(4 * 60_000 + 59_000))
        assertEquals("1h05m", minutesLabel(65 * 60_000L))
    }

    @Test
    fun clockCapsEachPageAndStopsCounting() {
        var now = 0L
        val recorded = mutableListOf<Long>()
        val clock = ReadingClock(recorded::add) { now }
        clock.lap() // 始める前は数えない
        clock.start()
        now = 30_000
        clock.lap()
        now += MAX_PAGE_MILLIS * 2 // 置きっぱなし
        clock.stop()
        now += 60_000
        clock.lap() // 止めたあとは数えない
        assertEquals(listOf(30_000L, MAX_PAGE_MILLIS), recorded)
    }
}
