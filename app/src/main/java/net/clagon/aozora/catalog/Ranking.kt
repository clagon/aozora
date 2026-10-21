package net.clagon.aozora.catalog

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.Reader

/**
 * 青空文庫公式のアクセスランキング（XHTML 版）。期間ごとに上位 500 作品の作品 ID を 1 位から並べる。
 * 期間は "2022"（通年）か "2022-05"（月）の形で、新しい期間から並ぶ。
 * 公式の更新は 2022 年で止まっているので、assets/ranking.tsv に写して持つ（取り直しは scripts/update-ranking.sh）。
 * 公式の表には、同じ作品が 1 つの期間に 2 度入っていることがある（作品のファイルごとに数えているため）。
 * 一覧には上の順位で 1 度だけ出し、下の順位は欠番にする。
 */
class Rankings(private val byPeriod: Map<String, List<Int>>) {
    /** 通年のランキングがある年。新しい年から。 */
    val years get() = byPeriod.keys.filter { '-' !in it }

    /** year 年のうちランキングがある月（1〜12）。新しい月から。 */
    fun months(year: String) = byPeriod.keys.filter { it.startsWith("$year-") }.map { it.substringAfter('-').toInt() }

    /** 一番新しい通年のランキング。ホームに出す。 */
    val latest get() = byPeriod.getValue(years.first())

    /** period のランキングの作品 ID を、公式の表のとおり（2 度入っている作品もそのまま）並べたもの。ない期間なら空。 */
    operator fun get(period: String) = byPeriod[period].orEmpty()

    /** period での作品 ID ごとの順位（1 から）。下の順位から入れ、2 度入っている作品は上の順位で上書きする。 */
    fun ranks(period: String) = get(period).withIndex().reversed().associate { it.value to it.index + 1 }
}

/** ranking.tsv を読む。1 行が「期間<TAB>作品 ID を , でつないだもの」。 */
fun readRankings(reader: Reader) = Rankings(
    reader.readLines().filter { it.isNotBlank() }.associate { line ->
        val (period, ids) = line.split('\t')
        period to ids.split(',').map(String::toInt)
    },
)

/** ランキング ids の上位 limit 件を順位の順に。2 度目に出てくる作品と、カタログにない作品は飛ばす。 */
fun CatalogDao.ranking(ids: List<Int>, limit: Int = ids.size): Flow<List<WorkItem>> {
    val top = ids.take(limit).distinct()
    return workItems(top).map { items ->
        val byId = items.associateBy { it.id }
        top.mapNotNull(byId::get)
    }
}
