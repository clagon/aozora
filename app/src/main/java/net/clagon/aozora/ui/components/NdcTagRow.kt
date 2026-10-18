package net.clagon.aozora.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import net.clagon.aozora.catalog.ndcTags

/** 分野タグの文字色と背景色。1 つ目を緑、2 つ目を橙にし、それ以降は繰り返す。 */
private val TAG_COLORS = listOf(Color(0xFF3FA36B) to Color(0xFFE6F4EC), Color(0xFFE8794A) to Color(0xFFFDEEE6))

/** 作品の分類（[ndcTags]）をタグで並べる。幅が足りなければ折り返す。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NdcTagRow(ndc: String) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        ndcTags(ndc).forEachIndexed { i, tag ->
            val (fg, bg) = TAG_COLORS[i % TAG_COLORS.size]
            Text(
                tag,
                color = fg,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.background(bg).padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
}
