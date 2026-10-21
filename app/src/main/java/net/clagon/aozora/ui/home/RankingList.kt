package net.clagon.aozora.ui.home

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import net.clagon.aozora.AozoraApp
import net.clagon.aozora.catalog.CatalogDao
import net.clagon.aozora.catalog.WorkItem
import net.clagon.aozora.catalog.ranking
import net.clagon.aozora.ui.components.ChoiceMenu
import net.clagon.aozora.ui.components.WorkList

/** 通年を表す月。 */
private const val WHOLE_YEAR = 0

/**
 * ランキングの一覧。先頭で年と月（通年か 1〜12 月）を選び、各行の左に順位を出す。最初は一番新しい年の通年を出す。
 * 年を変えたときは、その年にも同じ月があれば月を保ち、なければ通年に戻す。
 */
@Composable
fun RankingList(dao: CatalogDao, open: (WorkItem) -> Unit) {
    val rankings = (LocalContext.current.applicationContext as AozoraApp).rankings
    var year by rememberSaveable { mutableStateOf(rankings.years.first()) }
    var month by rememberSaveable { mutableIntStateOf(WHOLE_YEAR) }
    val period = if (month == WHOLE_YEAR) year else "%s-%02d".format(year, month)
    // 年や月を変えたら一覧を作り直し、先頭から見せる
    key(period) {
        val ranks = remember { rankings.ranks(period) }
        WorkList(remember { dao.ranking(rankings[period]) }, open, rank = { ranks[it.id] }, pinHeader = true) {
            Row(Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)) {
                ChoiceMenu(year, rankings.years, { "$it 年" }, "年を選ぶ") {
                    year = it
                    if (month !in rankings.months(it)) month = WHOLE_YEAR
                }
                ChoiceMenu(
                    month,
                    listOf(WHOLE_YEAR) + rankings.months(year),
                    { if (it == WHOLE_YEAR) "通年" else "$it 月" },
                    "月を選ぶ",
                ) { month = it }
            }
        }
    }
}
