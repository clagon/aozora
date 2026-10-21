package net.clagon.aozora.library

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.clagon.aozora.catalog.CatalogDao
import net.clagon.aozora.catalog.PersonItem
import net.clagon.aozora.catalog.WorkItem
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/** 本棚や読書履歴に並べる 1 冊。開いたことがなければ openedAt は null、本棚になければ addedAt は null。 */
data class Book(val work: WorkItem, val progress: Float, val openedAt: Long?, val addedAt: Long?)

/** 1 日の読書時間。 */
data class DayReading(val day: LocalDate, val millis: Long)

/** 本棚の並べ替え。 */
enum class ShelfOrder(val label: String) {
    Recent("最近開いた本"),
    Added("追加日"),
    Title("タイトル"),
}

/**
 * 本棚と読書履歴。利用者のデータ（[LibraryDao]）とカタログは別の DB なので、作品の情報はカタログから引いてつなぐ。
 * カタログから消えた作品は並べない。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class Library(val dao: LibraryDao, private val catalog: CatalogDao, private val scope: CoroutineScope) {
    /**
     * endedAgo ミリ秒前までの millis を、読んだ日の読書時間に足す。0 時をまたいでいれば 2 日に分ける。
     * リーダーを閉じるときにも呼ぶので、画面ではなくアプリのスコープで書く（画面のスコープは閉じると止まり、書き込みが消える）。
     */
    fun addReadingTime(millis: Long, endedAgo: Long) {
        val days = splitAtMidnight(LocalDateTime.now().minus(Duration.ofMillis(endedAgo)), millis)
        scope.launch { days.forEach { (day, ms) -> dao.addReadingTime(day.toString(), ms) } }
    }

    /** today までの 7 日の読書時間。古い日から順に、読んでいない日も 0 で並べる。 */
    fun lastSevenDays(today: LocalDate): Flow<List<DayReading>> =
        dao.readingTimes(today.minusDays(6).toString()).map { lastSevenDays(today, it) }

    fun shelf(order: ShelfOrder): Flow<List<Book>> = dao.shelf().flatMapLatest { rows ->
        catalog.workItems(rows.map { it.workId }).map { works ->
            val byId = works.associateBy { it.id }
            rows.mapNotNull { r -> byId[r.workId]?.let { Book(it, r.progress ?: 0f, r.openedAt, r.addedAt) } }
                .sortedFor(order)
        }
    }

    /** お気に入り作家。カタログから消えた作家は並べない。 */
    fun favoritePersons(): Flow<List<PersonItem>> = dao.favoritePersons().flatMapLatest { catalog.personItems(it) }

    fun history(): Flow<List<Book>> = dao.history().flatMapLatest { rows ->
        catalog.workItems(rows.map { it.workId }).map { works ->
            val byId = works.associateBy { it.id }
            rows.mapNotNull { r -> byId[r.workId]?.let { Book(it, r.progress, r.openedAt, null) } }
        }
    }
}

/**
 * 本棚の並べ替え。「最近開いた本」は、まだ開いていない作品もあるので、最後に開いた日時と本棚に追加した日時の新しいほうで並べる。
 */
internal fun List<Book>.sortedFor(order: ShelfOrder): List<Book> = when (order) {
    ShelfOrder.Recent -> sortedByDescending { maxOf(it.openedAt ?: 0, it.addedAt ?: 0) }
    ShelfOrder.Added -> sortedByDescending { it.addedAt ?: 0 }
    ShelfOrder.Title -> sortedBy { it.work.sortYomi }
}

/** end に終わった millis（1 日より短い）を、日ごとの時間に分ける。0 時をまたいでいなければ 1 日だけ。 */
internal fun splitAtMidnight(end: LocalDateTime, millis: Long): Map<LocalDate, Long> {
    val start = end.minus(Duration.ofMillis(millis))
    if (start.toLocalDate() == end.toLocalDate()) return mapOf(end.toLocalDate() to millis)
    val midnight = end.toLocalDate().atStartOfDay()
    return mapOf(
        start.toLocalDate() to Duration.between(start, midnight).toMillis(),
        end.toLocalDate() to Duration.between(midnight, end).toMillis(),
    )
}

/** today までの 7 日を古い順に並べ、rows にない日は 0 にする。 */
internal fun lastSevenDays(today: LocalDate, rows: List<ReadingTime>): List<DayReading> {
    val byDay = rows.associate { it.day to it.millis }
    return (6 downTo 0).map { today.minusDays(it.toLong()) }.map { DayReading(it, byDay[it.toString()] ?: 0) }
}
