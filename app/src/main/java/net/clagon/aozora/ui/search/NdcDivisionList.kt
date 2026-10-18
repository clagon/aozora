package net.clagon.aozora.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.clagon.aozora.catalog.Catalog
import net.clagon.aozora.catalog.NDC_CLASSES
import net.clagon.aozora.ui.Screen

/** 第 1 次区分を開いた先の、第 2 次区分の一覧。 */
@Composable
fun NdcDivisionList(screen: Screen.Ndc, catalog: Catalog, push: (Screen) -> Unit) {
    // 件数は第 1 次区分のカードを出す前にそろっているので、ここで null になるのはカタログを読めないときだけ
    val counts = catalog.ndcCounts.collectAsState().value ?: return
    LazyColumn {
        items(NDC_CLASSES[screen.digit].codes) { (code, name) ->
            NdcDivisionRow(code, name, counts.of(code, screen.kidsOnly)) {
                push(Screen.NdcWorks(code, name, screen.kidsOnly))
            }
        }
    }
}

/** 第 2 次区分の行。番号・名前・件数を並べ、作品のない区分は薄くして押せなくする。 */
@Composable
private fun NdcDivisionRow(code: String, name: String, count: Int, onClick: () -> Unit) {
    val empty = count == 0
    Row(
        Modifier.fillMaxWidth().clickable(
            enabled = !empty,
            onClick = onClick,
        ).padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            code,
            color = MaterialTheme.colorScheme.outline,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.width(56.dp),
        )
        Text(
            name,
            color = if (empty) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        if (!empty) {
            Text(
                "%,d件".format(count),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}
