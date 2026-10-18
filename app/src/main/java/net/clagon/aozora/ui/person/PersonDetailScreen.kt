package net.clagon.aozora.ui.person

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.clagon.aozora.catalog.Catalog
import net.clagon.aozora.catalog.Person
import net.clagon.aozora.catalog.WorkItem
import net.clagon.aozora.ui.components.WorkList

// 「人物について」を畳んでいるときに見せる行数
private const val ABOUT_COLLAPSED_LINES = 3

/**
 * 作家詳細。読み・ローマ字・生没年・人物についての下に、役割を問わず関わった作品を並べる。
 * 作品を押すと open に渡す。
 */
// ponytail: お気に入り作家の ☆ は、利用者データの DB を作るときに右上に足す
@Composable
fun PersonDetailScreen(personId: Int, catalog: Catalog, open: (WorkItem) -> Unit) {
    val person by remember(personId) { catalog.dao.person(personId) }.collectAsState(null)
    // 取りに行っている間と、取りに行けなかったときは null で、欄ごと出さない
    val about by produceState<String?>(null, person?.id) { value = person?.let { catalog.about(it) } }
    WorkList(remember(personId) { catalog.dao.worksByPerson(personId) }, open) {
        person?.let { PersonHeader(it, about) }
    }
}

/** 作品一覧の上に置く、作家の項目。値が空の項目は出さない。 */
@Composable
private fun PersonHeader(person: Person, about: String?) {
    Column(
        Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Field("作家名読み", "${person.lastNameYomi} ${person.firstNameYomi}".trim())
        Field(
            "ローマ字表記",
            listOf(person.lastNameRoman, person.firstNameRoman).filter {
                it.isNotBlank()
            }.joinToString(", "),
        )
        Field("生年", person.born)
        Field("没年", person.died)
        if (!about.isNullOrBlank()) Field("人物について") { About(about) }
    }
}

/** 項目名と値の 1 行。 */
@Composable
private fun Field(label: String, value: String) {
    if (value.isNotBlank()) Field(label) { Text(value, style = MaterialTheme.typography.bodyMedium) }
}

@Composable
private fun Field(label: String, value: @Composable () -> Unit) {
    Row {
        Text(
            "$label：",
            color = MaterialTheme.colorScheme.outline,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(112.dp),
        )
        Column(Modifier.weight(1f)) { value() }
    }
}

/** 人物について。長いときは畳んでおき、「もっと」で全文を開く。 */
@Composable
private fun About(text: String) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var overflows by remember { mutableStateOf(false) }
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        maxLines = if (expanded) Int.MAX_VALUE else ABOUT_COLLAPSED_LINES,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = { if (!expanded) overflows = it.hasVisualOverflow },
    )
    if (overflows && !expanded) {
        Text(
            "もっと",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.clickable { expanded = true },
        )
    }
}
