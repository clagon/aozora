package net.clagon.aozora.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class KeywordSearchTest {
    @Test
    fun withSearchPutsTheQueryFirstOnce() {
        assertEquals(listOf("羅生門"), withSearch(emptyList(), " 羅生門 "))
        // 同じ語は先頭に移し、2 つにしない
        assertEquals(listOf("太宰", "羅生門"), withSearch(listOf("羅生門", "太宰"), "太宰"))
        // 空白だけなら残さない
        assertEquals(listOf("羅生門"), withSearch(listOf("羅生門"), "  "))
        // 改行は区切りと混ざらないよう空白にする
        assertEquals(listOf("走れ メロス"), withSearch(emptyList(), "走れ\nメロス"))
        // 古いものから落として 20 件までにする
        val full = (1..20).map { "$it" }
        assertEquals(listOf("new") + (1..19).map { "$it" }, withSearch(full, "new"))
    }
}
