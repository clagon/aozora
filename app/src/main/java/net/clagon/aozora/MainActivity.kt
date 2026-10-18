package net.clagon.aozora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext

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
    val catalog = (LocalContext.current.applicationContext as AozoraApp).catalog
    var tab by rememberSaveable { mutableStateOf(Tab.Home) }
    // ponytail: 画面の積み上げは回転などで作り直されると消える。困ったら Navigation を入れる
    val stack = remember { mutableStateListOf<Screen>() }
    val push: (Screen) -> Unit = { stack += it }
    val back = { stack.removeAt(stack.lastIndex) }
    BackHandler(enabled = stack.isNotEmpty()) { back() }
    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach {
                    NavigationBarItem(
                        selected = it == tab,
                        onClick = { tab = it; stack.clear() },
                        icon = { Icon(it.icon, contentDescription = null) },
                        label = { Text(it.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            val top = stack.lastOrNull()
            when {
                top != null -> StackedScreen(top, catalog.dao, push) { back() }
                tab == Tab.Home -> HomeScreen(catalog, push)
                tab == Tab.Library -> LibraryScreen(push)
                // ponytail: 本棚とその他の中身は未実装。それまではその他にカタログの状態を出しておく
                tab == Tab.Other -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CatalogStatus(catalog) }
                else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(tab.label) }
            }
        }
    }
}
