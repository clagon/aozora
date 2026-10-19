package net.clagon.aozora.ui.bookshelf

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.clagon.aozora.catalog.CatalogDao
import net.clagon.aozora.library.Book
import net.clagon.aozora.ui.Screen

/**
 * 本棚の作品を長押ししたときに下から出すシート。「書籍情報」で作品詳細、「作家情報」で作家詳細を開き、「削除」で本棚から外す。
 * 作家が複数（著者と翻訳者など）いるときは、「作家情報」を押すと役割つきの名前から選ばせる。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookActions(book: Book, catalog: CatalogDao, push: (Screen) -> Unit, onRemove: () -> Unit, onDismiss: () -> Unit) {
    val credits by remember(book) { catalog.creditsOf(book.work.id) }.collectAsState(emptyList())
    var choosingPerson by remember { mutableStateOf(false) }
    val open = { screen: Screen ->
        onDismiss()
        push(screen)
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            book.work.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        )
        if (choosingPerson) {
            credits.forEach {
                val label = if (it.role == "著者") it.name else "${it.name}（${it.role}）"
                Action(label) { open(Screen.PersonDetail(it.personId, it.name)) }
            }
        } else {
            Action("書籍情報") { open(Screen.WorkDetail(book.work.id)) }
            Action("作家情報") {
                val only = credits.singleOrNull()
                if (only != null) open(Screen.PersonDetail(only.personId, only.name)) else choosingPerson = true
            }
            Action("削除") {
                onRemove()
                onDismiss()
            }
        }
        Spacer(Modifier.navigationBarsPadding().height(16.dp))
    }
}

@Composable
private fun Action(label: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    )
}
