package net.clagon.aozora.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import net.clagon.aozora.catalog.CatalogDao
import net.clagon.aozora.catalog.KanaRow
import net.clagon.aozora.catalog.PersonItem
import net.clagon.aozora.ui.Screen

/** 作家別。左に五十音の行の縦タブ、右に選んだ行の作家一覧を並べる。 */
@Composable
fun PersonBrowser(dao: CatalogDao, push: (Screen) -> Unit) {
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
