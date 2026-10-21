package net.clagon.aozora.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.clagon.aozora.catalog.CatalogDao
import net.clagon.aozora.catalog.WorkItem
import net.clagon.aozora.ui.components.ChoiceMenu
import net.clagon.aozora.ui.components.WorkList

/** 新着作品の一覧。先頭の年を押して公開年を選び、各行には公開日を月日で添える。最初は一番新しい年を出す。 */
@Composable
fun NewWorksList(dao: CatalogDao, open: (WorkItem) -> Unit) {
    val years by remember { dao.publishedYears() }.collectAsState(null)
    var chosen by rememberSaveable { mutableStateOf<String?>(null) }
    val year = chosen ?: years?.firstOrNull() ?: return
    // 年を変えたら一覧を作り直し、先頭から見せる
    key(year) {
        WorkList(
            remember { dao.newWorksIn(year) },
            open,
            extra = { work ->
                Text(
                    publishedMonthDay(work.publishedOn),
                    color = MaterialTheme.colorScheme.outline,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            },
        ) {
            Box(Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)) {
                ChoiceMenu(year, years.orEmpty(), { "$it 年" }, "公開年を選ぶ") { chosen = it }
            }
        }
    }
}

/** "2026-04-03" を「4 月 3 日公開」にする。形が違えばそのまま返す。 */
internal fun publishedMonthDay(date: String): String {
    val (month, day) = date.split("-").drop(1).mapNotNull { it.toIntOrNull() }.takeIf { it.size == 2 } ?: return date
    return "$month 月 $day 日公開"
}
