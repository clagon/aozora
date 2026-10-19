package net.clagon.aozora.ui.work

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryAddCheck
import androidx.compose.material.icons.outlined.LibraryAdd
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.clagon.aozora.AozoraApp
import net.clagon.aozora.catalog.Catalog
import net.clagon.aozora.catalog.SourceBook
import net.clagon.aozora.ui.Screen
import net.clagon.aozora.ui.components.BookCover
import net.clagon.aozora.ui.components.NdcTagRow

/**
 * 作品詳細。表紙・作品名・作家・分野タグの下に、作品データ・底本データ・工作員データを並べる。
 * 作家名を押すとその作家の作品一覧を開く。
 */
@Composable
fun WorkDetailScreen(workId: Int, catalog: Catalog, push: (Screen) -> Unit) {
    val work by remember(workId) { catalog.dao.work(workId) }.collectAsState(null)
    val item by remember(workId) { catalog.dao.workItem(workId) }.collectAsState(null)
    val credits by remember(workId) { catalog.dao.creditsOf(workId) }.collectAsState(emptyList())
    // 取りに行っている間と、取りに行けなかったときは null で、欄ごと出さない
    val about by produceState<String?>(null, work?.id) { value = work?.let { catalog.about(it) } }
    val library = (LocalContext.current.applicationContext as AozoraApp).library.dao
    val onShelf by remember(workId) { library.onShelf(workId) }.collectAsState(false)
    val scope = rememberCoroutineScope()
    val w = work ?: return
    val i = item ?: return

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Row(
                Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                BookCover(i, Modifier.width(96.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(w.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (w.subtitle.isNotBlank()) {
                        Text(
                            w.subtitle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    credits.forEach { person ->
                        // 著者以外（翻訳者など）は役割を添えて、著者と見分けられるようにする
                        val label = if (person.role == "著者") person.name else "${person.name}（${person.role}）"
                        Text(
                            label,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.clickable { push(Screen.PersonDetail(person.personId, person.name)) },
                        )
                    }
                    NdcTagRow(w.ndc)
                }
            }
            Section("作品データ") {
                Field("作品名読み", w.titleYomi)
                Field("分類", w.ndc)
                Field("文字遣い種別", w.kanaType)
                Field("作品について", about.orEmpty())
            }
            listOfNotNull(w.source1, w.source2).forEach { Section("底本データ") { SourceFields(it) } }
            Section("工作員データ") {
                Field("入力", w.inputter)
                Field("校正", w.proofreader)
            }
            Spacer(Modifier.height(24.dp))
        }
        BottomBar(
            onShelf,
            toggleShelf = {
                scope.launch { if (onShelf) library.removeFromShelf(w.id) else library.addToShelf(w.id) }
            },
            read = { push(Screen.Reader(w.id)) },
        )
    }
}

/** 下部のボタン。左に本棚への追加（本棚にあれば押すと外す）、右に「今すぐ読む」。 */
@Composable
private fun BottomBar(onShelf: Boolean, toggleShelf: () -> Unit, read: () -> Unit) {
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
    Row(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = toggleShelf)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                if (onShelf) Icons.Filled.LibraryAddCheck else Icons.Outlined.LibraryAdd,
                contentDescription = null,
                tint = if (onShelf) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                if (onShelf) "追加済み" else "本棚に追加",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Button(onClick = read, modifier = Modifier.padding(start = 12.dp).weight(1f).height(52.dp)) {
            Text("今すぐ読む", style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** 区画。上に灰色の帯を挟んで、見出しと項目を並べる。 */
@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxWidth().height(10.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh))
    Column(Modifier.padding(horizontal = 16.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        content()
    }
}

/** 項目名と値の 1 行。値が空の項目は出さない。 */
@Composable
private fun Field(label: String, value: String) {
    if (value.isBlank()) return
    Row {
        Text(
            label,
            color = MaterialTheme.colorScheme.outline,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(112.dp),
        )
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

/** 底本の項目。 */
@Composable
private fun SourceFields(source: SourceBook) {
    Field("底本", source.name)
    Field("出版社", source.publisher)
    Field("初版発行日", source.firstEdition)
    Field("入力に使用", source.inputEdition)
    Field("校正に使用", source.proofEdition)
}
