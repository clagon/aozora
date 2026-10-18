package net.clagon.aozora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import net.clagon.aozora.catalog.Catalog
import net.clagon.aozora.catalog.CatalogState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                App()
            }
        }
    }
}

private enum class Tab(val label: String, val icon: ImageVector) {
    Home("ホーム", Icons.Filled.Home),
    Bookshelf("本棚", Icons.AutoMirrored.Filled.List),
    Library("文庫", Icons.Filled.Search),
    Other("その他", Icons.Filled.Menu),
}

@Composable
private fun App() {
    var tab by rememberSaveable { mutableStateOf(Tab.Home) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach {
                    NavigationBarItem(
                        selected = it == tab,
                        onClick = { tab = it },
                        icon = { Icon(it.icon, contentDescription = null) },
                        label = { Text(it.label) },
                    )
                }
            }
        },
    ) { padding ->
        // ponytail: 各タブの中身は未実装。画面を作るときに差し替える。
        // それまではホームとその他にカタログの状態を出しておく
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            when (tab) {
                Tab.Home, Tab.Other -> CatalogStatus((LocalContext.current.applicationContext as AozoraApp).catalog)
                else -> Text(tab.label)
            }
        }
    }
}

@Composable
private fun CatalogStatus(catalog: Catalog) {
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
