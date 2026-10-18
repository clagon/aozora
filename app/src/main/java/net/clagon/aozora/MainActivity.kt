package net.clagon.aozora

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
        )
        setContent {
            // ponytail: 元アプリと同じく端末のダークモードには従わない。その他画面のカラーモードを作るときに切り替えられるようにする
            MaterialTheme(colorScheme = lightColorScheme(primary = Accent, background = Color.White, surface = Color.White, surfaceContainerHigh = Color(0xFFF0F2F4))) {
                App()
            }
        }
    }
}

private val Accent = Color(0xFF4A86E8)

private enum class Tab(val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    Home("ホーム", Icons.Outlined.Home, Icons.Filled.Home),
    Bookshelf("本棚", Icons.Outlined.ViewAgenda, Icons.Filled.ViewAgenda),
    Library("文庫", Icons.AutoMirrored.Outlined.MenuBook, Icons.AutoMirrored.Filled.MenuBook),
    Other("その他", Icons.Filled.MoreHoriz, Icons.Filled.MoreHoriz),
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
        bottomBar = { Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                Tab.entries.forEach {
                    NavigationBarItem(
                        selected = it == tab,
                        onClick = { tab = it; stack.clear() },
                        icon = { Icon(if (it == tab) it.selectedIcon else it.icon, contentDescription = null) },
                        label = { Text(it.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.outline,
                            unselectedTextColor = MaterialTheme.colorScheme.outline,
                            indicatorColor = Color.Transparent,
                        ),
                    )
                }
            }
        } },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            val top = stack.lastOrNull()
            when {
                top != null -> StackedScreen(top, catalog.dao, push) { back() }
                tab == Tab.Home -> HomeScreen(catalog, push)
                tab == Tab.Library -> LibraryScreen(catalog.dao, push)
                // ponytail: 本棚とその他の中身は未実装。それまではその他にカタログの状態を出しておく
                tab == Tab.Other -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CatalogStatus(catalog) }
                else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(tab.label) }
            }
        }
    }
}
