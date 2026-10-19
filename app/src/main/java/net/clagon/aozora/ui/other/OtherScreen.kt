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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import net.clagon.aozora.catalog.Catalog
import net.clagon.aozora.catalog.CatalogState
import net.clagon.aozora.ui.Screen

/**
 * その他タブ。灰色の地に白い角丸のカードを並べる。
 * お気に入りの作家の一覧と、カタログ（作品数と取り直し）を置く。
 */
// ponytail: 読書時間のグラフ、カラーモード、設定（画面を消灯しない・音量キーでめくる）は、それぞれ作るときに足す
@Composable
fun OtherScreen(catalog: Catalog, push: (Screen) -> Unit) {
    val state by catalog.state.collectAsState()
    val count by catalog.workCount.collectAsState(initial = null)
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card {
            Item("お気に入りの作家", onClick = { push(Screen.FavoritePersons) })
        }
        Card {
            Item("作品数", value = count?.let { "%,d 件".format(it) }.orEmpty())
            HorizontalDivider(
                Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            )
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
