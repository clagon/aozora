package net.clagon.aozora.reader

/** 本文の見出し。level は 0 が大見出し、1 が中見出し、2 が小見出し。page は 0 から数える。 */
data class Heading(val level: Int, val title: String, val page: Int)

/** reader.js が 1 行に 1 つ「階層<TAB>ページ<TAB>見出し」の形で渡す見出しを読む。形の崩れた行は飛ばす。 */
fun parseHeadings(text: String): List<Heading> = text.lines().mapNotNull { line ->
    val (level, page, title) = line.split('\t', limit = 3).takeIf { it.size == 3 } ?: return@mapNotNull null
    Heading(level.toIntOrNull() ?: return@mapNotNull null, title, page.toIntOrNull() ?: return@mapNotNull null)
}

/**
 * 「前の章」で移る先のページ。今のページより前で、いちばん近い見出しのページ。
 * 章の途中にいるならその章の頭に、章の頭にいるならひとつ前の章に移る。なければ null。
 */
fun List<Heading>.previousChapter(page: Int): Int? = lastOrNull { it.page < page }?.page

/** 「次の章」で移る先のページ。今のページより後で、いちばん近い見出しのページ。なければ null。 */
fun List<Heading>.nextChapter(page: Int): Int? = firstOrNull { it.page > page }?.page

/** 今のページが属する見出し。最初の見出しより前なら null。 */
fun List<Heading>.current(page: Int): Heading? = lastOrNull { it.page <= page }
