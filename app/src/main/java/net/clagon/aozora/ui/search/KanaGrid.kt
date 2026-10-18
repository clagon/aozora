package net.clagon.aozora.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import net.clagon.aozora.catalog.KANA_GRID
import net.clagon.aozora.ui.Screen

/** 作品別。五十音の 1 文字ずつのボタンを 5 列の格子に並べる。 */
@Composable
fun KanaGrid(push: (Screen) -> Unit) {
    LazyVerticalGrid(
        GridCells.Fixed(5),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(KANA_GRID) { kana ->
            if (kana == null) {
                Spacer(Modifier.aspectRatio(1.4f))
            } else {
                Box(
                    Modifier.aspectRatio(1.4f).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh).clickable { push(Screen.KanaWorks(kana)) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(kana, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
