package net.clagon.aozora.ui.bookshelf

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import kotlinx.coroutines.launch
import net.clagon.aozora.catalog.CatalogDao
import net.clagon.aozora.library.Book
import net.clagon.aozora.library.Library
import net.clagon.aozora.library.ShelfOrder
import net.clagon.aozora.ui.Screen
import net.clagon.aozora.ui.components.BookCover
import net.clagon.aozora.ui.components.SwipeToRemove
import net.clagon.aozora.ui.components.TextTabs
import net.clagon.aozora.ui.components.WorkRow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** 表示方式と並べ替えを残す SharedPreferences の名前。 */
private const val PREFS = "bookshelf"

/** 並べ替えのメニューの項目に添えるアイコン。 */
private val ORDER_ICONS = mapOf(
    ShelfOrder.Recent to Icons.Filled.Schedule,
    ShelfOrder.Added to Icons.Filled.Event,
    ShelfOrder.Title to Icons.Filled.SortByAlpha,
)

private val DATE = DateTimeFormatter.ofPattern("yyyy/MM/dd")

/**
 * 本棚タブ。上部のタブか左右のスワイプで本棚と読書履歴を切り替える。作品を押すとリーダーで開く。
 * 本棚は表示方式（グリッド／リスト）と並べ替えを右上のメニューで選び、長押しで作品詳細・作家詳細・削除を出す。
 * 読書履歴は最後に開いた順に並べ、左へスワイプすると履歴から外す。右上のごみ箱で、確かめてからすべて外す。
 */
@Composable
fun BookshelfScreen(library: Library, catalog: CatalogDao, push: (Screen) -> Unit) {
    val prefs = LocalContext.current.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    val pager = rememberPagerState { 2 }
    val onShelfTab = pager.currentPage == 0
    var list by remember { mutableStateOf(prefs.getBoolean("list", false)) }
    var order by remember {
        mutableStateOf(
            ShelfOrder.entries.firstOrNull {
                it.name == prefs.getString("order", null)
            } ?: ShelfOrder.Recent,
        )
    }
    var menuShown by remember { mutableStateOf(false) }
    var clearAsked by remember { mutableStateOf(false) }
    LaunchedEffect(list, order) { prefs.edit { putBoolean("list", list).putString("order", order.name) } }
    // メニューの項目。選ぶとメニューを閉じる
    @Composable
    fun MenuChoice(label: String, icon: ImageVector, checked: Boolean, select: () -> Unit) = DropdownMenuItem(
        text = { Text(label) },
        onClick = {
            select()
            menuShown = false
        },
        leadingIcon = { Icon(icon, contentDescription = null) },
        trailingIcon = { if (checked) Icon(Icons.Filled.Check, contentDescription = "選択中") },
    )
    var pressed by remember { mutableStateOf<Book?>(null) }
    val read = { book: Book -> push(Screen.Reader(book.work.id)) }
    val scope = rememberCoroutineScope()

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextTabs(listOf("本棚", "読書履歴"), pager, Modifier.weight(1f))
            if (!onShelfTab) {
                IconButton(onClick = { clearAsked = true }) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "読書履歴をすべて消す",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Box {
                if (onShelfTab) {
                    IconButton(onClick = { menuShown = true }) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = "メニュー",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                // 右端のボタンから出すので、画面の端に貼りつかないよう少し内側に寄せる
                DropdownMenu(
                    menuShown,
                    onDismissRequest = { menuShown = false },
                    offset = DpOffset((-12).dp, 0.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    MenuHeader("表示方式")
                    MenuChoice("グリッド", Icons.Filled.GridView, !list) { list = false }
                    MenuChoice("リスト", Icons.AutoMirrored.Filled.ViewList, list) { list = true }
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    MenuHeader("並べ替え")
                    ShelfOrder.entries.forEach {
                        MenuChoice(it.label, ORDER_ICONS.getValue(it), it == order) {
                            order =
                                it
                        }
                    }
                }
            }
        }
        HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
            if (page == 0) {
                val books by remember(order) { library.shelf(order) }.collectAsState(null)
                Shelf(books ?: return@HorizontalPager, list, read) { pressed = it }
            } else {
                val books by remember { library.history() }.collectAsState(null)
                History(books ?: return@HorizontalPager, library, read)
            }
        }
    }
    if (clearAsked) {
        AlertDialog(
            onDismissRequest = { clearAsked = false },
            title = { Text("読書履歴をすべて消しますか") },
            text = { Text("本棚と、作品ごとの読んだ位置はそのまま残ります。") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { library.dao.clearHistory() }
                    clearAsked = false
                }) { Text("消す") }
            },
            dismissButton = { TextButton(onClick = { clearAsked = false }) { Text("やめる") } },
        )
    }
    pressed?.let { book ->
        BookActions(
            book,
            catalog,
            push,
            onRemove = { scope.launch { library.dao.removeFromShelf(book.work.id) } },
            onDismiss = { pressed = null },
        )
    }
}

/** 本棚。グリッドなら表紙を 3 列に並べて下に読了率の点、リストなら 1 行ずつ読了率と最後に開いた日を出す。 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Shelf(books: List<Book>, list: Boolean, read: (Book) -> Unit, longPress: (Book) -> Unit) {
    if (books.isEmpty()) return Empty("本棚に作品がありません\n作品詳細の「本棚に追加」で追加できます")
    val press = { book: Book -> Modifier.combinedClickable(onLongClick = { longPress(book) }) { read(book) } }
    if (list) {
        LazyColumn(Modifier.fillMaxSize()) {
            items(books, key = { it.work.id }) { WorkRow(it.work, press(it)) { BookStatus(it) } }
        }
    } else {
        LazyVerticalGrid(
            GridCells.Fixed(3),
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            items(books, key = { it.work.id }) {
                Column(press(it)) {
                    BookCover(it.work, Modifier.fillMaxWidth())
                    ProgressDots(it.progress)
                }
            }
        }
    }
}

/** 読書履歴。左へスワイプすると履歴から外す（読書位置は残る）。 */
@Composable
private fun History(books: List<Book>, library: Library, read: (Book) -> Unit) {
    if (books.isEmpty()) return Empty("まだ開いた作品がありません")
    val scope = rememberCoroutineScope()
    LazyColumn(Modifier.fillMaxSize()) {
        items(books, key = { it.work.id }) { book ->
            SwipeToRemove({ scope.launch { library.dao.removeFromHistory(book.work.id) } }) {
                WorkRow(book.work, Modifier.clickable { read(book) }) {
                    BookStatus(book)
                }
            }
        }
    }
}

/** 読了率と最後に開いた日。 */
@Composable
private fun BookStatus(book: Book) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Text(
            "読了率: ${"%.2f".format(book.progress * 100)}%",
            color = MaterialTheme.colorScheme.outline,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.weight(1f))
        book.openedAt?.let {
            Text(
                DATE.format(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault())),
                color = MaterialTheme.colorScheme.outline,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/** 表紙の下の読了率。10 個の点のうち、読んだ割合の分を塗る。 */
@Composable
fun ProgressDots(progress: Float) {
    val filled = (progress * 10).toInt()
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        repeat(10) {
            val color = if (it < filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
            Box(Modifier.size(5.dp).clip(CircleShape).background(color))
        }
    }
}

/** メニューの中の見出し。 */
@Composable
private fun MenuHeader(label: String) {
    Text(
        label,
        color = MaterialTheme.colorScheme.outline,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@Composable
private fun Empty(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            message,
            color = MaterialTheme.colorScheme.outline,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}
