package net.clagon.aozora.library

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import net.clagon.aozora.catalog.CatalogDao
import net.clagon.aozora.catalog.PersonItem
import net.clagon.aozora.catalog.WorkItem

/** 本棚や読書履歴に並べる 1 冊。開いたことがなければ openedAt は null、本棚になければ addedAt は null。 */
data class Book(val work: WorkItem, val progress: Float, val openedAt: Long?, val addedAt: Long?)

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
class Library(val dao: LibraryDao, private val catalog: CatalogDao) {
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
