package net.clagon.aozora.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.clagon.aozora.catalog.Catalog
import net.clagon.aozora.catalog.NDC_CLASSES
import net.clagon.aozora.catalog.WorkItem
import net.clagon.aozora.catalog.kanaRange
import net.clagon.aozora.ui.components.WorkList
import net.clagon.aozora.ui.person.FavoritePersons
import net.clagon.aozora.ui.person.FavoriteStar
import net.clagon.aozora.ui.person.PersonDetailScreen
import net.clagon.aozora.ui.reader.ReaderScreen
import net.clagon.aozora.ui.search.NdcDivisionList
import net.clagon.aozora.ui.work.WorkDetailScreen

/** タブの上に積む画面。戻る操作で 1 つずつ外す。 */
sealed interface Screen {
    /** 上部の見出しに出す文字。 */
    val title: String

    /** ホームの「もっと見る」で開く、新着作品の全件。 */
    data object NewWorks : Screen {
        override val title = "新着作品"
    }

    /** 分野の第 2 次区分の一覧。kidsOnly なら児童書だけを数える。 */
    data class Ndc(val digit: Int, val kidsOnly: Boolean) : Screen {
        override val title get() = NDC_CLASSES[digit].name
    }

    /** 第 2 次区分（code は 2 桁）に属する作品の一覧。 */
    data class NdcWorks(val code: String, override val title: String, val kidsOnly: Boolean) : Screen

    /** 作品別で選んだ 1 文字で始まる作品の一覧。 */
    data class KanaWorks(val kana: String) : Screen {
        override val title get() = "作品一覧：$kana"
    }

    /** 作品詳細。見出しは出さず、作品名は本文の上部に大きく出す。 */
    data class WorkDetail(val workId: Int) : Screen {
        override val title = ""
    }

    /** 本文を読むリーダー。見出しや戻るボタンの枠を持たず、画面全体を使う。 */
    data class Reader(val workId: Int) : Screen {
        override val title = ""
    }

    /** 作家詳細。見出しは作家名で、右上に ☆ を置く。 */
    data class PersonDetail(val personId: Int, override val title: String) : Screen

    /** その他タブから開く、お気に入り作家の一覧。 */
    data object FavoritePersons : Screen {
        override val title = "お気に入りの作家"
    }
}

/** 積んだ画面の共通の枠。上部に戻るボタンと見出しを置き、中身を画面の種類で切り替える。 */
@Composable
fun StackedScreen(screen: Screen, catalog: Catalog, push: (Screen) -> Unit, back: () -> Unit) {
    if (screen is Screen.Reader) return ReaderScreen(screen.workId, catalog, back)
    val dao = catalog.dao
    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        Box(Modifier.fillMaxWidth().height(64.dp), contentAlignment = Alignment.Center) {
            IconButton(onClick = back, modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBackIos,
                    contentDescription = "戻る",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                screen.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 56.dp),
            )
            if (screen is Screen.PersonDetail) {
                FavoriteStar(screen.personId, Modifier.align(Alignment.CenterEnd).padding(end = 4.dp))
            }
        }
        val open = { work: WorkItem -> push(Screen.WorkDetail(work.id)) }
        when (screen) {
            Screen.NewWorks -> WorkList(remember { dao.newWorks(Int.MAX_VALUE) }, open)

            is Screen.Ndc -> NdcDivisionList(screen, catalog, push)

            is Screen.NdcWorks -> WorkList(remember(screen) { dao.worksByNdc(screen.code, screen.kidsOnly) }, open)

            is Screen.KanaWorks -> WorkList(
                remember(screen) {
                    kanaRange(screen.kana).let { dao.worksByYomi(it.from, it.to) }
                },
                open,
            )

            is Screen.PersonDetail -> PersonDetailScreen(screen.personId, catalog, open)

            is Screen.WorkDetail -> WorkDetailScreen(screen.workId, catalog, push)

            Screen.FavoritePersons -> FavoritePersons(push)

            is Screen.Reader -> {}
        }
    }
}
