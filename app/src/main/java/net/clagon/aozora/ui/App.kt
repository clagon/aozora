package net.clagon.aozora.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import net.clagon.aozora.AozoraApp
import net.clagon.aozora.catalog.Catalog
import net.clagon.aozora.ui.components.CatalogStatus
import net.clagon.aozora.ui.home.HomeScreen
import net.clagon.aozora.ui.search.SearchScreen

/** 画面を開く・戻るときのスライドの時間。 */
private const val SLIDE_MILLIS = 300

/** 下部タブ。 */
private enum class Tab(val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    Home("ホーム", Icons.Outlined.Home, Icons.Filled.Home),
    Bookshelf("本棚", Icons.Outlined.ViewAgenda, Icons.Filled.ViewAgenda),
    Search("探す", Icons.AutoMirrored.Outlined.MenuBook, Icons.AutoMirrored.Filled.MenuBook),
    Other("その他", Icons.Filled.MoreHoriz, Icons.Filled.MoreHoriz),
}

/** タブの画面の上に画面を積んで遷移する、アプリ全体の枠。 */
@Composable
fun App() {
    val catalog = (LocalContext.current.applicationContext as AozoraApp).catalog
    var tab by rememberSaveableEnum(Tab.Home)
    // ponytail: 画面の積み上げは回転などで作り直されると消える。困ったら Navigation を入れる
    val stack = remember { mutableStateListOf<Screen>() }
    val push: (Screen) -> Unit = { stack += it }
    val back = { stack.removeAt(stack.lastIndex) }
    BackHandler(enabled = stack.isNotEmpty()) { back() }
    // 画面を離れて戻ってきたときに、タブの選択やスクロール位置を残しておく
    val saveableState = rememberSaveableStateHolder()
    AnimatedContent(
        targetState = stack.toList(),
        transitionSpec = {
            val slide = tween<IntOffset>(SLIDE_MILLIS)
            if (targetState.size > initialState.size) {
                // 開く: 右からスライドインし、元の画面は少し左へずれる
                slideInHorizontally(slide) { it } togetherWith slideOutHorizontally(slide) { -it / 4 }
            } else {
                // 戻る: 今の画面が右へ抜け、下の画面が戻ってくる。抜ける画面を手前に描く
                (slideInHorizontally(slide) { -it / 4 } togetherWith slideOutHorizontally(slide) { it }).apply { targetContentZIndex = -1f }
            }
        },
        label = "screen",
    ) { screens ->
        val top = screens.lastOrNull()
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            saveableState.SaveableStateProvider(if (top == null) "root" else "${screens.size}:$top") {
                if (top != null) {
                    StackedScreen(top, catalog, push) { back() }
                } else {
                    TabRoot(tab, catalog, push) { tab = it }
                }
            }
        }
    }
}

/** 下部タブと、選んだタブの画面。積んだ画面がないときだけ表示される。 */
@Composable
private fun TabRoot(tab: Tab, catalog: Catalog, push: (Screen) -> Unit, select: (Tab) -> Unit) {
    Scaffold(
        bottomBar = { Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                Tab.entries.forEach {
                    NavigationBarItem(
                        selected = it == tab,
                        onClick = { select(it) },
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
            when (tab) {
                Tab.Home -> HomeScreen(catalog, push)
                Tab.Search -> SearchScreen(catalog, push)
                // ponytail: 本棚とその他の中身は未実装。それまではその他にカタログの状態を出しておく
                Tab.Other -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CatalogStatus(catalog) }
                Tab.Bookshelf -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(tab.label) }
            }
        }
    }
}
