package net.clagon.aozora.ui.search

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import net.clagon.aozora.catalog.Catalog
import net.clagon.aozora.ui.Screen
import net.clagon.aozora.ui.components.TextTabs

/** 探すタブの上部に並べる 4 つの探し方。並びは [SearchScreen] の分岐と対応する。 */
private val SEARCH_TABS = listOf("分野別", "作家別", "作品別", "児童書")

/** 探すタブ。上部のタブで分野別・作家別・作品別・児童書を切り替える。 */
@Composable
fun SearchScreen(catalog: Catalog, push: (Screen) -> Unit) {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val counts by catalog.ndcCounts.collectAsState()
    Column {
        TextTabs(SEARCH_TABS, selected, { selected = it })
        when (selected) {
            0 -> NdcGrid(counts, kidsOnly = false, push)
            1 -> PersonBrowser(catalog.dao, push)
            2 -> KanaGrid(push)
            else -> NdcGrid(counts, kidsOnly = true, push)
        }
    }
}
