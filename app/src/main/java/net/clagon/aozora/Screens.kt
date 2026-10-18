package net.clagon.aozora

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.TextButton
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

private const val HOME_NEW_WORKS = 10

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

@Composable
fun HomeScreen(catalog: Catalog, push: (Screen) -> Unit) {
    val state by catalog.state.collectAsState()
    val works by remember { catalog.dao.newWorks(HOME_NEW_WORKS) }.collectAsState(null)
    LazyColumn {
        if (state != CatalogState.Ready || works?.isEmpty() == true) {
            item { Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CatalogStatus(catalog) } }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("新着作品", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = { push(Screen.NewWorks) }) { Text("もっと見る") }
            }
        }
        workItems(works.orEmpty())
    }
}

private val LIBRARY_TABS = listOf("分野別", "作家別", "作品別", "児童書")

@Composable
fun LibraryScreen(push: (Screen) -> Unit) {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    Column {
        PrimaryTabRow(selectedTabIndex = selected) {
            LIBRARY_TABS.forEachIndexed { i, label ->
                Tab(selected = i == selected, onClick = { selected = i }, text = { Text(label) })
            }
        }
        LazyColumn {
            if (selected == 0) {
                items(NDC_CLASSES) { NavItem("${it.digit} ${it.name}") { push(Screen.Ndc(it.digit)) } }
            } else {
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
