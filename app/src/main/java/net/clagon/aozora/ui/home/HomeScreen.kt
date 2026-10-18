package net.clagon.aozora.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.clagon.aozora.catalog.Catalog
import net.clagon.aozora.catalog.CatalogState
import net.clagon.aozora.catalog.WorkItem
import net.clagon.aozora.ui.Screen
import net.clagon.aozora.ui.components.BookCover
import net.clagon.aozora.ui.components.CatalogStatus
import net.clagon.aozora.ui.components.NdcTagRow

// 1 件目を表紙と詳細で大きく出し、続く 3 件を表紙だけで並べる
private const val HOME_NEW_WORKS = 4

/** ホームタブ。検索の入口と新着作品を出す。 */
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
        works?.takeIf { it.isNotEmpty() }?.let { CoverShowcase(it) { work -> push(Screen.WorkDetail(work.id)) } }
    }
}

// ponytail: 検索はまだない。押しても何も起きない入口だけを置いている
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

/** 区画の見出しと、右端の「もっと見る」。 */
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

/** 1 件目は表紙と詳細を横に並べ、残りは表紙を 3 列で並べる。表紙の幅はどちらも 3 列の 1 マス分。押すと onClick に作品を渡す。 */
@Composable
private fun CoverShowcase(works: List<WorkItem>, onClick: (WorkItem) -> Unit) {
    val gap = 16.dp
    BoxWithConstraints {
        val cell = (maxWidth - gap * 2) / 3
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            val first = works.first()
            Row(Modifier.clickable { onClick(first) }, horizontalArrangement = Arrangement.spacedBy(gap)) {
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
                works.drop(1).forEach { BookCover(it, Modifier.width(cell).clickable { onClick(it) }) }
            }
        }
    }
}
