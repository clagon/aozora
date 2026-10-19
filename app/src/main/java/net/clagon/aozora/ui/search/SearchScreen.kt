package net.clagon.aozora.ui.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import net.clagon.aozora.catalog.Catalog
import net.clagon.aozora.ui.Screen
import net.clagon.aozora.ui.components.TextTabs

/** 探すタブの上部に並べる 4 つの探し方。並びは [SearchScreen] の分岐と対応する。 */
private val SEARCH_TABS = listOf("分野別", "作家別", "作品別", "児童書")

/** 探すタブ。上部のタブか左右のスワイプで、分野別・作家別・作品別・児童書を切り替える。 */
@Composable
fun SearchScreen(catalog: Catalog, push: (Screen) -> Unit) {
    val pager = rememberPagerState { SEARCH_TABS.size }
    val counts by catalog.ndcCounts.collectAsState()
    Column {
        TextTabs(SEARCH_TABS, pager)
        HorizontalPager(pager, Modifier.fillMaxSize(), verticalAlignment = Alignment.Top) { page ->
            when (page) {
                0 -> NdcGrid(counts, kidsOnly = false, push)
                1 -> PersonBrowser(catalog.dao, push)
                2 -> KanaGrid(push)
                else -> NdcGrid(counts, kidsOnly = true, push)
            }
        }
    }
}
