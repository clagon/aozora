package net.clagon.aozora.ui.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.clagon.aozora.reader.Heading

/**
 * 目次。見出しを階層ごとに字下げして並べ、押すとその見出しのページへ移る（onSelect）。
 * 今読んでいる見出しは色を変え、開いたときに見える位置に置く。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TocPanel(headings: List<Heading>, current: Heading?, onSelect: (Heading) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            "目次",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 20.dp, bottom = 8.dp),
        )
        if (headings.isEmpty()) {
            Text(
                "この作品には見出しがありません",
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 48.dp),
            )
            return@ModalBottomSheet
        }
        val state = rememberLazyListState(headings.indexOf(current).coerceAtLeast(0))
        LazyColumn(state = state) {
            items(headings) { heading ->
                val reading = heading == current
                val color = if (reading) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                Row(
                    Modifier.fillMaxWidth().clickable { onSelect(heading) }
                        .padding(start = 20.dp + 16.dp * heading.level, end = 20.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        heading.title,
                        color = color,
                        fontWeight = if (reading) FontWeight.Bold else FontWeight.Normal,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "${heading.page + 1}",
                        color = MaterialTheme.colorScheme.outline,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
