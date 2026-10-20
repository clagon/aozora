package net.clagon.aozora.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class NewWorksListTest {
    @Test
    fun publishedMonthDayDropsTheYearAndLeadingZeros() {
        assertEquals("4 月 3 日公開", publishedMonthDay("2020-04-03"))
        assertEquals("12 月 31 日公開", publishedMonthDay("2025-12-31"))
        // 形が違うときはそのまま出す
        assertEquals("", publishedMonthDay(""))
        assertEquals("2020/04/03", publishedMonthDay("2020/04/03"))
    }
}
