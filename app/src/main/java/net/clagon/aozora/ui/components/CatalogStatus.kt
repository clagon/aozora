package net.clagon.aozora.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import net.clagon.aozora.catalog.Catalog
import net.clagon.aozora.catalog.CatalogState

/**
 * カタログの取り込み状態・作品数と「カタログを更新」ボタン。
 * ホーム（取り込み中・失敗・空のときだけ）と、その他タブ（常に）に出す。
 */
@Composable
fun CatalogStatus(catalog: Catalog) {
    val state by catalog.state.collectAsState()
    val count by catalog.workCount.collectAsState(initial = null)
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (val s = state) {
            CatalogState.Updating -> {
                CircularProgressIndicator()
                Text("カタログを取り込んでいます…")
            }
            is CatalogState.Failed -> Text("カタログを取り込めませんでした\n${s.message}")
            CatalogState.Ready -> {}
        }
        count?.let { Text("作品 $it 件") }
        if (state != CatalogState.Updating) {
            Button(onClick = catalog::update) { Text("カタログを更新") }
        }
    }
}
