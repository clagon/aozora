package net.clagon.aozora.ui.components

import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * 画面上部の文字だけのタブ。選んでいるタブは太字にし、下に短い青線を引く。
 * 中身は pager の HorizontalPager に置き、タブを押すか左右にスワイプして切り替える。
 */
@Composable
fun TextTabs(labels: List<String>, pager: PagerState, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val selected = pager.currentPage
    PrimaryTabRow(
        selectedTabIndex = selected,
        modifier = modifier,
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
        labels.forEachIndexed { i, label ->
            Tab(
                selected = i == selected,
                onClick = { scope.launch { pager.animateScrollToPage(i) } },
                selectedContentColor = MaterialTheme.colorScheme.onSurface,
                unselectedContentColor = MaterialTheme.colorScheme.outline,
                text = {
                    Text(
                        label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (i == selected) FontWeight.Bold else FontWeight.Normal,
                    )
                },
            )
        }
    }
}
