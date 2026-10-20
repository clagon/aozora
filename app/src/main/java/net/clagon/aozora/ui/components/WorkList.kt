package net.clagon.aozora.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import net.clagon.aozora.catalog.WorkItem

/**
 * 作品の一覧。各行は小さな表紙・作品名・文字遣い種別・著者で、押すと onClick に作品を渡す。
 * header は一覧の先頭に置き、一覧と一緒にスクロールする。rank を渡すと、各行の左にその作品の順位を出す。
 * extra は各行の著者の下に足す行（[WorkRow] の extra）。
 */
@Composable
fun WorkList(
    works: Flow<List<WorkItem>>,
    onClick: (WorkItem) -> Unit,
    rank: ((WorkItem) -> Int?)? = null,
    extra: @Composable ColumnScope.(WorkItem) -> Unit = {},
    header: @Composable () -> Unit = {},
) {
    val items by works.collectAsState(null)
    when {
        items == null -> {}

        items!!.isEmpty() -> Column {
            header()
            Text("作品がありません", Modifier.padding(16.dp))
        }

        else -> LazyColumn {
            item { header() }
            items(items!!, key = { it.id }) { work ->
                WorkRow(work, Modifier.clickable { onClick(work) }, rank = rank?.invoke(work)) { extra(work) }
            }
        }
    }
}

/**
 * 作品の一覧の 1 行。小さな表紙・作品名・文字遣い種別・著者を並べる。押したときの動きは modifier で渡す。
 * extra は著者の下に足す行（本棚の読了率など）。rank を渡すと、表紙の左に順位を出す。
 */
@Composable
fun WorkRow(
    work: WorkItem,
    modifier: Modifier = Modifier,
    rank: Int? = null,
    extra: @Composable ColumnScope.() -> Unit = {},
) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (rank != null) {
            Text(
                "$rank",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(40.dp).padding(end = 8.dp),
            )
        }
        BookCover(work, Modifier.width(64.dp), small = true)
        Column(Modifier.padding(start = 16.dp).weight(1f)) {
            Text(
                work.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (work.subtitle.isNotBlank()) {
                Text(
                    work.subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(work.kanaType, color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.bodySmall)
            work.authors?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            extra()
        }
    }
}
