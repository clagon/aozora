package net.clagon.aozora.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.flowOf
import net.clagon.aozora.catalog.CatalogDao
import net.clagon.aozora.catalog.searchLike
import net.clagon.aozora.ui.Screen
import net.clagon.aozora.ui.components.WorkRow
import net.clagon.aozora.ui.search.personItems

/**
 * ホームの検索窓から開く検索画面。打つたびに作家名と作品名（読み・副題・原題を含む）から引き、作家・作品の順に並べる。
 * 平仮名・片仮名、全角・半角、空白の有無の違いは問わない（[net.clagon.aozora.catalog.searchKey]）。
 */
// ponytail: 当たった順ではなく読みの順に並べる。前方一致を先に出したくなったら並べ替えを足す
@Composable
fun KeywordSearch(dao: CatalogDao, push: (Screen) -> Unit, back: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val key = searchLike(query)
    // 何も打っていないときは全件に当たるので、DB を引かずに空にしておく
    val persons by remember(key) {
        if (key.isEmpty()) flowOf(emptyList()) else dao.searchPersons(key)
    }.collectAsState(null)
    val works by remember(key) {
        if (key.isEmpty()) flowOf(emptyList()) else dao.searchWorks(key)
    }.collectAsState(null)
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = remember { FocusRequester() }
    // 開いたらすぐ打てるようにする。作品を見て戻ってきたとき（打った文字があるとき）はキーボードを出さない
    LaunchedEffect(Unit) { if (query.isEmpty()) focus.requestFocus() }

    Column(Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
        Row(
            Modifier.padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = back) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBackIos,
                    contentDescription = "戻る",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextField(
                query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f).focusRequester(focus),
                placeholder = { Text("作品名、作家名") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = {
                            // 消したらすぐ打ち直せるよう、検索窓に入力を戻してキーボードを出す
                            query = ""
                            focus.requestFocus()
                            keyboard?.show()
                        }) { Icon(Icons.Filled.Close, contentDescription = "消す") }
                    }
                },
                singleLine = true,
                shape = CircleShape,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
            )
        }
        val foundPersons = persons.orEmpty()
        val foundWorks = works.orEmpty()
        when {
            key.isEmpty() || persons == null || works == null -> {}

            foundPersons.isEmpty() && foundWorks.isEmpty() -> Text(
                "見つかりませんでした",
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(24.dp),
            )

            else -> LazyColumn {
                if (foundPersons.isNotEmpty()) {
                    header("作家", foundPersons.size)
                    personItems(foundPersons, push)
                }
                if (foundWorks.isNotEmpty()) {
                    header("作品", foundWorks.size)
                    // 作家の行は作家 ID を key にしているので、作品は文字列にして重ならないようにする
                    items(foundWorks, key = { "work:${it.id}" }) {
                        WorkRow(it, Modifier.clickable { push(Screen.WorkDetail(it.id)) })
                    }
                }
            }
        }
    }
}

/** 検索結果の区切りの見出し。件数を添える。 */
private fun LazyListScope.header(label: String, count: Int) {
    stickyHeader(key = "header:$label") {
        Text(
            "$label（%,d 件）".format(count),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }
}
