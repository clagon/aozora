package net.clagon.aozora.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.clagon.aozora.catalog.CatalogDao
import net.clagon.aozora.catalog.NDC_CLASSES
import net.clagon.aozora.ui.Screen

/** 分野カードの色とアイコン。並びは NDC の第 1 次区分の順。 */
private val NDC_CARD_STYLES = listOf(
    Color(0xFF5BB5CC) to Icons.AutoMirrored.Filled.LibraryBooks,
    Color(0xFFE4B962) to Icons.Filled.Psychology,
    Color(0xFFC2876F) to Icons.Filled.HistoryEdu,
    Color(0xFFE8737C) to Icons.Filled.AccountBalance,
    Color(0xFF9089E0) to Icons.Filled.Science,
    Color(0xFFFFA03A) to Icons.Filled.Engineering,
    Color(0xFF86CC5C) to Icons.Filled.Forest,
    Color(0xFF68B5E5) to Icons.Filled.Palette,
    Color(0xFF35BDB0) to Icons.Filled.Public,
    Color(0xFF6A99E6) to Icons.Filled.Create,
)

/** 分野別・児童書の第 1 次区分のカードを 2 列で並べる。 */
@Composable
fun NdcGrid(dao: CatalogDao, kidsOnly: Boolean, push: (Screen) -> Unit) {
    LazyVerticalGrid(
        GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        items(NDC_CLASSES) { NdcCard(it.digit, it.name, dao, kidsOnly) { push(Screen.Ndc(it.digit, kidsOnly)) } }
    }
}

/** 第 1 次区分のカード。区分名・件数と、区分を表す薄いアイコンを出す。 */
@Composable
private fun NdcCard(digit: Int, name: String, dao: CatalogDao, kidsOnly: Boolean, onClick: () -> Unit) {
    val (color, icon) = NDC_CARD_STYLES[digit]
    val count by remember(digit, kidsOnly) { dao.ndcCount(digit.toString(), kidsOnly) }.collectAsState(null)
    // 作品のない分野（児童書で起きる）は、第 2 次区分と同じく薄くして押せなくする
    val empty = count == 0
    Box(
        Modifier.aspectRatio(1.66f).alpha(if (empty) 0.4f else 1f).clip(RoundedCornerShape(6.dp)).background(color)
            .clickable(enabled = count?.let { it > 0 } == true, onClick = onClick).padding(14.dp),
    ) {
        Column {
            Text(name, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Medium)
            count?.let { Text("%,d件".format(it), color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodyLarge) }
        }
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.25f), modifier = Modifier.align(Alignment.BottomEnd).size(56.dp))
    }
}
