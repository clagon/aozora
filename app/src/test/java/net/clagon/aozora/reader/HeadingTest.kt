package net.clagon.aozora.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HeadingTest {
    private val headings = parseHeadings("1\t0\t一\n1\t5\t二\nbroken\n2\t7\t二の一\n1\t9\t三 終わり")

    @Test
    fun parseHeadingsSkipsBrokenLines() {
        assertEquals(listOf("一", "二", "二の一", "三 終わり"), headings.map { it.title })
        assertEquals(emptyList<Heading>(), parseHeadings(""))
    }

    @Test
    fun chaptersMoveToTheNearestHeading() {
        // 章の途中なら章の頭へ、章の頭ならひとつ前の章へ
        assertEquals(5, headings.previousChapter(8))
        assertEquals(0, headings.previousChapter(5))
        assertNull(headings.previousChapter(0))
        assertEquals(9, headings.nextChapter(5))
        assertNull(headings.nextChapter(9))
        // 小見出し（二の一、7 ページ）は章を区切らないので止まらない
        assertEquals(9, headings.nextChapter(6))
        assertEquals("二の一", headings.current(8)?.title)
    }
}
