package net.clagon.aozora.ui.person

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.launch
import net.clagon.aozora.AozoraApp
import net.clagon.aozora.ui.Screen
import net.clagon.aozora.ui.search.personItems

/** 星の黄色。 */
private val STAR = Color(0xFFF5B400)

/** 作家詳細の右上の ☆。押すとお気に入り作家に入れたり外したりする。 */
@Composable
fun FavoriteStar(personId: Int, modifier: Modifier = Modifier) {
    val library = (LocalContext.current.applicationContext as AozoraApp).library.dao
    val favorite by remember(personId) { library.isFavorite(personId) }.collectAsState(false)
    val scope = rememberCoroutineScope()
    IconButton(
        onClick = {
            scope.launch { if (favorite) library.removeFavorite(personId) else library.addFavorite(personId) }
        },
        modifier = modifier,
    ) {
        Icon(
            if (favorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
            contentDescription = if (favorite) "お気に入りから外す" else "お気に入りに入れる",
            tint = if (favorite) STAR else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** お気に入り作家の一覧。並びと行の形は作家別と同じで、押すと作家詳細を開く。 */
@Composable
fun FavoritePersons(push: (Screen) -> Unit) {
    val library = (LocalContext.current.applicationContext as AozoraApp).library
    val persons by remember { library.favoritePersons() }.collectAsState(null)
    when {
        persons == null -> {}

        persons!!.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "お気に入りの作家はいません\n作家詳細の右上の ☆ で追加できます",
                color = MaterialTheme.colorScheme.outline,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }

        else -> LazyColumn(Modifier.fillMaxSize()) { personItems(persons!!, push) }
    }
}
