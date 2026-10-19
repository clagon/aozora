package net.clagon.aozora.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** 画面上部の文字だけのタブ。選んでいるタブは太字にし、下に短い青線を引く。 */
@Composable
fun TextTabs(labels: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
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
                onClick = { onSelect(i) },
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
