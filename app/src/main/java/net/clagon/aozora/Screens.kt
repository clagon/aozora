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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import kotlin.random.Random
import net.clagon.aozora.catalog.ndcTags
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import net.clagon.aozora.catalog.Catalog
import net.clagon.aozora.catalog.CatalogDao
import net.clagon.aozora.catalog.CatalogState
import net.clagon.aozora.catalog.KanaRow
import net.clagon.aozora.catalog.NDC_CLASSES
import net.clagon.aozora.catalog.WorkItem

/** タブの上に積む画面。戻る操作で 1 つずつ外す。 */
sealed interface Screen {
    val title: String

    data object NewWorks : Screen {
        override val title = "新着作品"
    }

    data class Ndc(val digit: Int) : Screen {
        override val title get() = NDC_CLASSES[digit].name
    }

    data class NdcWorks(val code: String, override val title: String) : Screen
    data class Persons(val row: KanaRow) : Screen {
        override val title get() = "作家（${row.label}行）"
    }

    data class Works(val row: KanaRow, val kidsOnly: Boolean) : Screen {
        override val title get() = "${if (kidsOnly) "児童書" else "作品"}（${row.label}行）"
    }

    // ponytail: 作家詳細ができるまでの仮の画面。作品を役割ごとに分けずに並べる
    data class PersonWorks(val personId: Int, override val title: String) : Screen
}

@Composable
fun StackedScreen(screen: Screen, dao: CatalogDao, push: (Screen) -> Unit, back: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る") }
            Text(screen.title, style = MaterialTheme.typography.titleLarge)
        }
        when (screen) {
            Screen.NewWorks -> WorkList(remember { dao.newWorks(Int.MAX_VALUE) })
            is Screen.Ndc -> LazyColumn {
                items(NDC_CLASSES[screen.digit].codes) { (code, name) ->
                    NavItem("$code $name") { push(Screen.NdcWorks(code, name)) }
                }
            }
            is Screen.NdcWorks -> WorkList(remember(screen) { dao.worksByNdc(screen.code) })
            is Screen.Works -> WorkList(remember(screen) { dao.worksByYomi(screen.row.from, screen.row.to, screen.kidsOnly) })
            is Screen.PersonWorks -> WorkList(remember(screen) { dao.worksByPerson(screen.personId) })
            is Screen.Persons -> {
                val persons by remember(screen) { dao.personsByYomi(screen.row.from, screen.row.to) }.collectAsState(null)
                LazyColumn {
                    items(persons.orEmpty(), key = { it.id }) {
                        val name = "${it.lastName} ${it.firstName}".trim()
                        NavItem(name) { push(Screen.PersonWorks(it.id, name)) }
                    }
                }
            }
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
private val PAPER_FLECK = Color(0x40A08C70)

/** 和紙風の表紙。上に作品名、下の帯に著者名を置き、細い線を 2 本ずつ添える。 */
@Composable
fun BookCover(work: WorkItem, modifier: Modifier = Modifier) {
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
        Box(Modifier.weight(0.68f).fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 24.dp), contentAlignment = Alignment.TopCenter) {
            Text(work.title, color = ink, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, style = MaterialTheme.typography.titleSmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
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
            Text(work.authors.orEmpty(), color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 6.dp))
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
        if (selected == 0) {
            LazyVerticalGrid(
                GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                items(NDC_CLASSES) { NdcCard(it.digit, it.name, dao) { push(Screen.Ndc(it.digit)) } }
            }
        } else {
            LazyColumn {
                items(KanaRow.entries) { row ->
                    NavItem("${row.label}行") {
                        push(if (selected == 1) Screen.Persons(row) else Screen.Works(row, kidsOnly = selected == 3))
                    }
                }
            }
        }
    }
}

@Composable
private fun NdcCard(digit: Int, name: String, dao: CatalogDao, onClick: () -> Unit) {
    val (color, icon) = NDC_CARD_STYLES[digit]
    val count by remember(digit) { dao.ndcClassCount(digit.toString()) }.collectAsState(null)
    Box(Modifier.aspectRatio(1.66f).clip(RoundedCornerShape(6.dp)).background(color).clickable(onClick = onClick).padding(14.dp)) {
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

@Composable
private fun WorkList(works: Flow<List<WorkItem>>) {
    val items by works.collectAsState(null)
    when {
        items == null -> {}
        items!!.isEmpty() -> Text("作品がありません", Modifier.padding(16.dp))
        else -> LazyColumn { workItems(items!!) }
    }
}

// ponytail: 作品詳細ができるまでは押しても何もしない
private fun LazyListScope.workItems(works: List<WorkItem>) {
    items(works, key = { it.id }) {
        ListItem(
            headlineContent = { Text(it.title) },
            supportingContent = { Text(listOfNotNull(it.subtitle.ifEmpty { null }, it.authors).joinToString("\n")) },
        )
        HorizontalDivider()
    }
}

@Composable
private fun NavItem(label: String, onClick: () -> Unit) {
    ListItem(headlineContent = { Text(label) }, modifier = Modifier.clickable(onClick = onClick))
    HorizontalDivider()
}
