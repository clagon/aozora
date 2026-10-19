package net.clagon.aozora.ui.other

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import net.clagon.aozora.AozoraApp
import net.clagon.aozora.catalog.Catalog
import net.clagon.aozora.catalog.CatalogState
import net.clagon.aozora.reader.ReaderSettings
import net.clagon.aozora.ui.Screen
import java.time.LocalDate

/**
 * その他タブ。灰色の地に白い角丸のカードを並べる。
 * 直近 7 日の読書時間、お気に入りの作家の一覧、読書の設定、カタログ（作品数と取り直し）を置く。
 */
// ponytail: カラーモードは、夜間・背景を作るときに足す
@Composable
fun OtherScreen(catalog: Catalog, push: (Screen) -> Unit) {
    val context = LocalContext.current
    val library = (context.applicationContext as AozoraApp).library
    val state by catalog.state.collectAsState()
    val count by catalog.workCount.collectAsState(initial = null)
    // 開いたまま日付が変わっても今日までの 7 日になるよう、前に出るたびに今日を確かめ直す
    var today by remember { mutableStateOf(LocalDate.now()) }
    LifecycleResumeEffect(Unit) {
        today = LocalDate.now()
        onPauseOrDispose {}
    }
    val days by remember(today) { library.lastSevenDays(today) }.collectAsState(emptyList())
    // その他タブを開くたびに読み直す（リーダーの文字パネルでも同じ設定を書き換えるため）
    var settings by remember { mutableStateOf(ReaderSettings.load(context)) }
    val change = { new: ReaderSettings ->
        settings = new
        new.save(context)
    }
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card {
            Text(
                "読書時間",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp),
            )
            ReadingTimeChart(days, Modifier.padding(16.dp))
        }
        Card {
            Item("お気に入りの作家", onClick = { push(Screen.FavoritePersons) })
        }
        Card {
            Toggle("読書中は画面を消灯しない", settings.keepScreenOn) { change(settings.copy(keepScreenOn = it)) }
            Divider()
            Toggle("音量キーでページをめくる", settings.volumeKeys) { change(settings.copy(volumeKeys = it)) }
        }
        Card {
            Item("作品数", value = count?.let { "%,d 件".format(it) }.orEmpty())
            Divider()
            when (val s = state) {
                CatalogState.Updating -> Item("カタログを取り込んでいます…")

                CatalogState.Ready -> Item("カタログを更新", onClick = catalog::update)

                is CatalogState.Failed -> {
                    Item("カタログを更新", value = "失敗しました", onClick = catalog::update)
                    Text(
                        s.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                    )
                }
            }
        }
    }
}

/** 白い角丸のカード。 */
@Composable
private fun Card(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface)) {
        content()
    }
}

/** カードの行の区切り。 */
@Composable
private fun Divider() =
    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh)

/** スイッチのついたカードの 1 行。行のどこを押しても切り替わる。 */
@Composable
private fun Toggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().toggleable(checked, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked, onCheckedChange = null)
    }
}

/** カードの 1 行。右に値を添え、押せる行には > を出す。 */
@Composable
private fun Item(label: String, value: String = "", onClick: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.bodyMedium)
        if (onClick != null) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}
