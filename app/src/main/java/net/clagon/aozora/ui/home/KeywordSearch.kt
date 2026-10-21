package net.clagon.aozora.ui.home

import android.content.Context
import android.content.SharedPreferences
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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import net.clagon.aozora.catalog.CatalogDao
import net.clagon.aozora.catalog.PersonItem
import net.clagon.aozora.catalog.searchLike
import net.clagon.aozora.ui.Screen
import net.clagon.aozora.ui.components.WorkRow
import net.clagon.aozora.ui.search.personItems

/**
 * ホームの検索窓から開く検索画面。打つたびに作家名と作品名（読み・副題・原題を含む）から引き、作家・作品の順に並べる。
 * 平仮名・片仮名、全角・半角、空白の有無の違いは問わない（[net.clagon.aozora.catalog.searchKey]）。
 * 名前が検索語と同じもの、検索語で始まるもの、途中に含むものの順に並べる。
 * 何も打っていない間は、最近の検索を出す。検索キーを押したとき、結果から作品・作家を開いたとき、履歴から選んだときに、その語を履歴の先頭に置く。
 */
@Composable
fun KeywordSearch(dao: CatalogDao, push: (Screen) -> Unit, back: () -> Unit) {
    // 履歴から選んだときにカーソルを末尾に置けるよう、文字とカーソルをまとめて持つ
    var field by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    val query = field.text
    val key = searchLike(query)
    val prefs = LocalContext.current.getSharedPreferences(HISTORY_PREFS, Context.MODE_PRIVATE)
    var history by remember { mutableStateOf(prefs.loadHistory()) }
    val changeHistory = { new: List<String> ->
        history = new
        prefs.edit { putString(HISTORY_KEY, new.joinToString("\n")) }
    }
    val saveSearch = { changeHistory(withSearch(history, query)) }
    val open = { screen: Screen ->
        saveSearch()
        push(screen)
    }
    // 作家と作品は、いつも同じ検索語の結果をそろえて出す。打ち変えている間は、新しい結果が出るまで前の結果を出しておく
    // （毎回消すと 1 文字ごとにちらつく）。何も打っていないときは全件に当たるので、DB を引かない
    val found by remember(key) {
        if (key.isEmpty()) flowOf(null) else combine(dao.searchPersons(key), dao.searchWorks(key), ::Pair)
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
                field,
                onValueChange = { field = it },
                modifier = Modifier.weight(1f).focusRequester(focus),
                placeholder = { Text("作品名、作家名") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = {
                            // 消したらすぐ打ち直せるよう、検索窓に入力を戻してキーボードを出す
                            field = TextFieldValue()
                            focus.requestFocus()
                            keyboard?.show()
                        }) { Icon(Icons.Filled.Close, contentDescription = "消す") }
                    }
                },
                singleLine = true,
                shape = CircleShape,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    saveSearch()
                    keyboard?.hide()
                }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
            )
        }
        val (foundPersons, foundWorks) = found ?: (emptyList<PersonItem>() to emptyList())
        when {
            key.isEmpty() -> SearchHistory(
                history,
                select = {
                    field = TextFieldValue(it, TextRange(it.length))
                    changeHistory(withSearch(history, it))
                    keyboard?.hide()
                },
                remove = { item -> changeHistory(history - item) },
                clear = { changeHistory(emptyList()) },
            )

            found == null -> {}

            foundPersons.isEmpty() && foundWorks.isEmpty() -> Text(
                "見つかりませんでした",
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(24.dp),
            )

            else -> LazyColumn {
                if (foundPersons.isNotEmpty()) {
                    header("作家", foundPersons.size)
                    personItems(foundPersons, open)
                }
                if (foundWorks.isNotEmpty()) {
                    header("作品", foundWorks.size)
                    // 作家の行は作家 ID を key にしているので、作品は文字列にして重ならないようにする
                    items(foundWorks, key = { "work:${it.id}" }) {
                        WorkRow(it, Modifier.clickable { open(Screen.WorkDetail(it.id)) })
                    }
                }
            }
        }
    }
}

/** 最近の検索を残す SharedPreferences の名前と鍵。新しいものから改行でつないで 1 つの文字列にする。 */
private const val HISTORY_PREFS = "search"
private const val HISTORY_KEY = "history"

/** 最近の検索として残す件数。 */
private const val HISTORY_MAX = 20

private fun SharedPreferences.loadHistory() = getString(HISTORY_KEY, null)?.split("\n")?.filter {
    it.isNotEmpty()
}.orEmpty()

/** 最近の検索に query を足した結果。前後の空白を除いて先頭に置き、同じ語は 1 つにまとめ、[HISTORY_MAX] 件までにする。 */
internal fun withSearch(history: List<String>, query: String): List<String> {
    // 改行は 1 行に入らないが、貼り付けで紛れたときに履歴の区切りと混ざらないよう空白にする
    val item = query.replace('\n', ' ').trim()
    if (item.isEmpty()) return history
    return (listOf(item) + (history - item)).take(HISTORY_MAX)
}

/** 何も打っていない間に出す、最近の検索。押すとその語で引き直す。 */
@Composable
private fun SearchHistory(
    history: List<String>,
    select: (String) -> Unit,
    remove: (String) -> Unit,
    clear: () -> Unit,
) {
    if (history.isEmpty()) return
    LazyColumn {
        item {
            Row(Modifier.padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "最近の検索",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = clear) { Text("すべて消す") }
            }
        }
        items(history, key = { it }) {
            Row(
                Modifier.fillMaxWidth().clickable { select(it) }.padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.History, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                Text(
                    it,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                )
                IconButton(onClick = { remove(it) }) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "履歴から消す",
                        tint = MaterialTheme.colorScheme.outline,
                    )
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
