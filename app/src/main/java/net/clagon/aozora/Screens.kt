package net.clagon.aozora

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.TabRowDefaults
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import kotlin.random.Random
import net.clagon.aozora.catalog.ndcTags
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.Flow
import net.clagon.aozora.catalog.Catalog
import net.clagon.aozora.catalog.CatalogDao
import net.clagon.aozora.catalog.CatalogState
import net.clagon.aozora.catalog.KANA_GRID
import net.clagon.aozora.catalog.KanaRow
import net.clagon.aozora.catalog.kanaRange
import net.clagon.aozora.catalog.NDC_CLASSES
import net.clagon.aozora.catalog.PersonItem
import net.clagon.aozora.catalog.WorkItem

/** タブの上に積む画面。戻る操作で 1 つずつ外す。 */
sealed interface Screen {
    val title: String

    data object NewWorks : Screen {
        override val title = "新着作品"
    }

    /** 分野の第 2 次区分の一覧。kidsOnly なら児童書だけを数える。 */
    data class Ndc(val digit: Int, val kidsOnly: Boolean) : Screen {
        override val title get() = NDC_CLASSES[digit].name
    }

    data class NdcWorks(val code: String, override val title: String, val kidsOnly: Boolean) : Screen

    /** 作品別で選んだ 1 文字で始まる作品の一覧。 */
    data class KanaWorks(val kana: String) : Screen {
        override val title get() = "作品一覧：$kana"
    }

    // ponytail: 作家詳細ができるまでの仮の画面。作品を役割ごとに分けずに並べる
    data class PersonWorks(val personId: Int, override val title: String) : Screen
}

@Composable
fun StackedScreen(screen: Screen, dao: CatalogDao, push: (Screen) -> Unit, back: () -> Unit) {
    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        Box(Modifier.fillMaxWidth().height(64.dp), contentAlignment = Alignment.Center) {
            IconButton(onClick = back, modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBackIos, contentDescription = "戻る", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(screen.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 56.dp))
        }
        when (screen) {
            Screen.NewWorks -> WorkList(remember { dao.newWorks(Int.MAX_VALUE) })
            is Screen.Ndc -> LazyColumn {
                items(NDC_CLASSES[screen.digit].codes) { (code, name) ->
                    NdcDivisionRow(code, name, dao, screen.kidsOnly) { push(Screen.NdcWorks(code, name, screen.kidsOnly)) }
                }
            }
            is Screen.NdcWorks -> WorkList(remember(screen) { dao.worksByNdc(screen.code, screen.kidsOnly) })
            is Screen.KanaWorks -> WorkList(remember(screen) { kanaRange(screen.kana).let { dao.worksByYomi(it.from, it.to) } })
            is Screen.PersonWorks -> WorkList(remember(screen) { dao.worksByPerson(screen.personId) })
        }
    }
}

// 元アプリのホームと同じく、1 件目を大きく、続く 3 件を表紙だけで並べる
private const val HOME_NEW_WORKS = 4

@Composable
fun HomeScreen(catalog: Catalog, push: (Screen) -> Unit) {
    val state by catalog.state.collectAsState()
    val works by remember { catalog.dao.newWorks(HOME_NEW_WORKS) }.collectAsState(null)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        SearchBarPlaceholder()
        if (state != CatalogState.Ready || works?.isEmpty() == true) {
            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CatalogStatus(catalog) }
        }
        SectionHeader("新着作品") { push(Screen.NewWorks) }
        works?.takeIf { it.isNotEmpty() }?.let { CoverShowcase(it) }
    }
}

// ponytail: 検索はまだない。見た目だけ元アプリに合わせて置いている
@Composable
private fun SearchBarPlaceholder() {
    Row(
        Modifier.padding(top = 12.dp).fillMaxWidth().height(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("作品名、作家名", Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SectionHeader(title: String, onMore: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 28.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Row(Modifier.clickable(onClick = onMore).padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("もっと見る", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyLarge)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

/** 1 件目は表紙と詳細を横に並べ、残りは表紙を 3 列で並べる。表紙の幅はどちらも 3 列の 1 マス分。 */
@Composable
private fun CoverShowcase(works: List<WorkItem>) {
    val gap = 16.dp
    BoxWithConstraints {
        val cell = (maxWidth - gap * 2) / 3
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            val first = works.first()
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                BookCover(first, Modifier.width(cell))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(first.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    first.authors?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleMedium) }
                    if (first.firstAppearance.isNotEmpty()) {
                        Text("初出：${first.firstAppearance}", color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    }
                    NdcTagRow(first.ndc)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                works.drop(1).forEach { BookCover(it, Modifier.width(cell)) }
            }
        }
    }
}

private val TAG_COLORS = listOf(Color(0xFF3FA36B) to Color(0xFFE6F4EC), Color(0xFFE8794A) to Color(0xFFFDEEE6))

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NdcTagRow(ndc: String) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        ndcTags(ndc).forEachIndexed { i, tag ->
            val (fg, bg) = TAG_COLORS[i % TAG_COLORS.size]
            Text(tag, color = fg, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.background(bg).padding(horizontal = 8.dp, vertical = 2.dp))
        }
    }
}

/** 表紙の色。文字色と帯の色の組。作品 ID で決めるので、同じ作品はいつも同じ色になる。 */
private val COVER_COLORS = listOf(
    Color(0xFFD9534F) to Color(0xFFE8897D),
    Color(0xFFC77DB5) to Color(0xFFDBA5D0),
    Color(0xFF4FAE5A) to Color(0xFF8AC98E),
    Color(0xFF8C7F74) to Color(0xFFB5A99E),
    Color(0xFF4F86C6) to Color(0xFF8FB4E0),
)
private val PAPER = Color(0xFFEBE6DC)

// 一覧の小さな表紙（幅 64dp）の文字。元アプリの一覧と並べて大きさを合わせた
private val SMALL_COVER_TITLE = TextStyle(fontSize = 9.5.sp, lineHeight = 13.sp)
private val SMALL_COVER_AUTHOR = TextStyle(fontSize = 8.5.sp, lineHeight = 11.sp)
private val PAPER_FLECK = Color(0x40A08C70)

/** 和紙風の表紙。上に作品名、下の帯に著者名を置き、細い線を 2 本ずつ添える。 */
@Composable
fun BookCover(work: WorkItem, modifier: Modifier = Modifier, small: Boolean = false) {
    val (ink, band) = COVER_COLORS[work.id.mod(COVER_COLORS.size)]
    Column(
        modifier.aspectRatio(0.72f).shadow(1.dp, RoundedCornerShape(3.dp)).background(PAPER).drawBehind {
            val random = Random(work.id)
            repeat(40) {
                val x = random.nextFloat() * size.width
                val y = random.nextFloat() * size.height * 0.68f
                if (it % 4 == 0) {
                    drawLine(PAPER_FLECK, Offset(x, y), Offset(x + 6.dp.toPx() * random.nextFloat(), y + 3.dp.toPx() * (random.nextFloat() - 0.5f)), 0.6.dp.toPx())
                } else {
                    drawCircle(PAPER_FLECK, 0.6.dp.toPx(), Offset(x, y))
                }
            }
            val stroke = 1.5.dp.toPx()
            drawLine(band, Offset(0f, 4.dp.toPx()), Offset(size.width * 0.36f, 4.dp.toPx()), stroke)
            drawLine(band, Offset(0f, 9.dp.toPx()), Offset(size.width * 0.2f, 9.dp.toPx()), stroke)
        },
    ) {
        Box(Modifier.weight(0.68f).fillMaxWidth().padding(horizontal = if (small) 4.dp else 8.dp).padding(top = if (small) 14.dp else 24.dp), contentAlignment = Alignment.TopCenter) {
            Text(work.title, color = ink, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, style = if (small) SMALL_COVER_TITLE else MaterialTheme.typography.titleSmall, maxLines = if (small) 2 else 3, overflow = TextOverflow.Ellipsis)
        }
        Box(
            Modifier.weight(0.32f).fillMaxWidth().background(band).drawBehind {
                val stroke = 1.5.dp.toPx()
                val line = ink.copy(alpha = 0.5f)
                drawLine(line, Offset(size.width * 0.62f, size.height - 9.dp.toPx()), Offset(size.width, size.height - 9.dp.toPx()), stroke)
                drawLine(line, Offset(size.width * 0.8f, size.height - 4.dp.toPx()), Offset(size.width, size.height - 4.dp.toPx()), stroke)
            },
            contentAlignment = Alignment.Center,
        ) {
            Text(work.authors.orEmpty(), color = Color.White.copy(alpha = 0.9f), style = if (small) SMALL_COVER_AUTHOR else MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 6.dp))
        }
    }
}

private val LIBRARY_TABS = listOf("分野別", "作家別", "作品別", "児童書")

/** 分野カードの色とアイコン。並びは NDC の第 1 次区分の順。 */
private val NDC_CARD_STYLES = listOf(
    Color(0xFF5BB5CC) to Icons.AutoMirrored.Filled.LibraryBooks,
    Color(0xFFE4B962) to Icons.Filled.Psychology,
    Color(0xFFC2876F) to Icons.Filled.HistoryEdu,
    Color(0xFFE8737C) to Icons.Filled.AccountBalance,
    Color(0xFF9089E0) to Icons.Filled.Science,
    Color(0xFFFFA03A) to Icons.Filled.Engineering,
    Color(0xFF86CC5C) to Icons.Filled.Forest,
    Color(0xFF68B5E5) to Icons.Filled.Palette,
    Color(0xFF35BDB0) to Icons.Filled.Public,
    Color(0xFF6A99E6) to Icons.Filled.Create,
)

@Composable
fun LibraryScreen(dao: CatalogDao, push: (Screen) -> Unit) {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    Column {
        PrimaryTabRow(
            selectedTabIndex = selected,
            containerColor = MaterialTheme.colorScheme.surface,
            divider = {},
            indicator = {
                TabRowDefaults.PrimaryIndicator(
                    Modifier.tabIndicatorOffset(selected, matchContentSize = true),
                    width = 32.dp,
                    shape = RoundedCornerShape(2.dp),
                )
            },
        ) {
            LIBRARY_TABS.forEachIndexed { i, label ->
                Tab(
                    selected = i == selected,
                    onClick = { selected = i },
                    selectedContentColor = MaterialTheme.colorScheme.onSurface,
                    unselectedContentColor = MaterialTheme.colorScheme.outline,
                    text = { Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = if (i == selected) FontWeight.Bold else FontWeight.Normal) },
                )
            }
        }
        when (selected) {
            0 -> NdcGrid(dao, kidsOnly = false, push)
            1 -> PersonBrowser(dao, push)
            2 -> KanaGrid(push)
            else -> NdcGrid(dao, kidsOnly = true, push)
        }
    }
}

@Composable
private fun NdcGrid(dao: CatalogDao, kidsOnly: Boolean, push: (Screen) -> Unit) {
    LazyVerticalGrid(
        GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        items(NDC_CLASSES) { NdcCard(it.digit, it.name, dao, kidsOnly) { push(Screen.Ndc(it.digit, kidsOnly)) } }
    }
}

/** 作家別。左に五十音の行の縦タブ、右に選んだ行の作家一覧を並べる。 */
@Composable
private fun PersonBrowser(dao: CatalogDao, push: (Screen) -> Unit) {
    var selected by rememberSaveable { mutableStateOf(KanaRow.A) }
    val persons by remember(selected) { dao.personsByYomi(selected.range.from, selected.range.to) }.collectAsState(null)
    Row(Modifier.fillMaxSize()) {
        Column(Modifier.width(88.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surfaceContainerHigh).verticalScroll(rememberScrollState())) {
            KanaRow.entries.forEach { row ->
                val isSelected = row == selected
                Box(
                    Modifier.fillMaxWidth().height(60.dp)
                        .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                        .clickable { selected = row },
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        Box(Modifier.align(Alignment.CenterStart).width(3.dp).height(24.dp).background(MaterialTheme.colorScheme.primary))
                    }
                    Text(row.label, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        // 行を切り替えたら一覧の先頭に戻す
        LazyColumn(Modifier.weight(1f), state = remember(selected) { LazyListState() }) {
            // 読みの頭文字ごとに見出しを挟む。見出しはスクロール中も上端に残り、今どのあたりかを示す
            persons.orEmpty().groupBy { it.lastNameSort.take(1) }.forEach { (initial, group) ->
                stickyHeader(key = "header:$initial") {
                    Text(
                        initial,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
                personItems(group, push)
            }
        }
    }
}

private fun LazyListScope.personItems(persons: List<PersonItem>, push: (Screen) -> Unit) {
    items(persons, key = { it.id }) {
        Row(
            Modifier.fillMaxWidth().clickable { push(Screen.PersonWorks(it.id, it.name)) }.padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(it.name, style = MaterialTheme.typography.titleMedium)
                Text("公開中：${it.workCount}", color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
    }
}

/** 作品別。五十音の 1 文字ずつのボタンを 5 列の格子に並べる。 */
@Composable
private fun KanaGrid(push: (Screen) -> Unit) {
    LazyVerticalGrid(
        GridCells.Fixed(5),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(KANA_GRID) { kana ->
            if (kana == null) {
                Spacer(Modifier.aspectRatio(1.4f))
            } else {
                Box(
                    Modifier.aspectRatio(1.4f).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh).clickable { push(Screen.KanaWorks(kana)) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(kana, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun NdcCard(digit: Int, name: String, dao: CatalogDao, kidsOnly: Boolean, onClick: () -> Unit) {
    val (color, icon) = NDC_CARD_STYLES[digit]
    val count by remember(digit, kidsOnly) { dao.ndcCount(digit.toString(), kidsOnly) }.collectAsState(null)
    // 作品のない分野（児童書で起きる）は、第 2 次区分と同じく薄くして押せなくする
    val empty = count == 0
    Box(
        Modifier.aspectRatio(1.66f).alpha(if (empty) 0.4f else 1f).clip(RoundedCornerShape(6.dp)).background(color)
            .clickable(enabled = count?.let { it > 0 } == true, onClick = onClick).padding(14.dp),
    ) {
        Column {
            Text(name, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Medium)
            count?.let { Text("%,d件".format(it), color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodyLarge) }
        }
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.25f), modifier = Modifier.align(Alignment.BottomEnd).size(56.dp))
    }
}

@Composable
fun CatalogStatus(catalog: Catalog) {
    val state by catalog.state.collectAsState()
    val count by catalog.workCount.collectAsState(initial = null)
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (val s = state) {
            CatalogState.Updating -> {
                CircularProgressIndicator()
                Text("カタログを取り込んでいます…")
            }
            is CatalogState.Failed -> Text("カタログを取り込めませんでした\n${s.message}")
            CatalogState.Ready -> {}
        }
        count?.let { Text("作品 $it 件") }
        if (state != CatalogState.Updating) {
            Button(onClick = catalog::update) { Text("カタログを更新") }
        }
    }
}

/** 作品の一覧。各行は小さな表紙・作品名・文字遣い種別・著者。 */
@Composable
private fun WorkList(works: Flow<List<WorkItem>>) {
    val items by works.collectAsState(null)
    when {
        items == null -> {}
        items!!.isEmpty() -> Text("作品がありません", Modifier.padding(16.dp))
        else -> LazyColumn {
            // ponytail: 作品詳細ができるまでは押しても何もしない
            items(items!!, key = { it.id }) { work ->
                Row(Modifier.fillMaxWidth().padding(end = 16.dp, top = 8.dp, bottom = 8.dp, start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    BookCover(work, Modifier.width(64.dp), small = true)
                    Column(Modifier.padding(start = 16.dp).weight(1f)) {
                        Text(work.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (work.subtitle.isNotBlank()) Text(work.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(work.kanaType, color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.bodySmall)
                        work.authors?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp)) }
                    }
                }
            }
        }
    }
}

/** 第 2 次区分の行。番号・名前・件数を並べ、作品のない区分は薄くして押せなくする。 */
@Composable
private fun NdcDivisionRow(code: String, name: String, dao: CatalogDao, kidsOnly: Boolean, onClick: () -> Unit) {
    val count by remember(code, kidsOnly) { dao.ndcCount(code, kidsOnly) }.collectAsState(null)
    val empty = count == 0
    Row(
        Modifier.fillMaxWidth().clickable(enabled = count?.let { it > 0 } == true, onClick = onClick).padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(code, color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.width(56.dp))
        Text(
            name,
            color = if (empty) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        count?.takeIf { it > 0 }?.let { Text("%,d件".format(it), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), style = MaterialTheme.typography.bodyMedium) }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant)
    }
}
