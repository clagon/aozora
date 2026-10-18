package net.clagon.aozora.ui.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.clagon.aozora.catalog.CatalogDao
import net.clagon.aozora.ui.Screen

/** 探すタブの上部に並べる 4 つの探し方。並びは [SearchScreen] の分岐と対応する。 */
private val SEARCH_TABS = listOf("分野別", "作家別", "作品別", "児童書")

/** 探すタブ。上部のタブで分野別・作家別・作品別・児童書を切り替える。 */
@Composable
fun SearchScreen(dao: CatalogDao, push: (Screen) -> Unit) {
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
            SEARCH_TABS.forEachIndexed { i, label ->
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
