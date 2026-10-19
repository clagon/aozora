package net.clagon.aozora.library

import net.clagon.aozora.catalog.WorkItem
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryTest {
    private fun book(title: String, yomi: String, openedAt: Long?, addedAt: Long) =
        Book(WorkItem(title.hashCode(), title, yomi, "", "", "", "", null), 0f, openedAt, addedAt)

    // あ: 古く追加して最近開いた。い: 開いていないが最近追加した。う: 中ほどに追加して開いていない
    private val books = listOf(book("う", "う", null, 20), book("い", "い", null, 30), book("あ", "あ", 40, 10))

    @Test
    fun recentUsesTheNewerOfOpenedAndAdded() {
        assertEquals(listOf("あ", "い", "う"), books.sortedFor(ShelfOrder.Recent).map { it.work.title })
    }

    @Test
    fun addedAndTitle() {
        assertEquals(listOf("い", "う", "あ"), books.sortedFor(ShelfOrder.Added).map { it.work.title })
        assertEquals(listOf("あ", "い", "う"), books.sortedFor(ShelfOrder.Title).map { it.work.title })
    }
}
