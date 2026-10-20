package net.clagon.aozora.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.StringReader

class CatalogTest {
    private fun csv(text: String) = readCsv(StringReader(text)).toList()

    @Test
    fun readCsvHandlesQuotesCommasNewlinesAndBom() {
        assertEquals(
            listOf(listOf("a", "b,c", "d\"e", ""), listOf("x\ny", "2020-01-01")),
            csv("\uFEFFa,\"b,c\",\"d\"\"e\",\r\n\"x\ny\",2020-01-01\r\n"),
        )
        assertEquals(listOf(listOf("last", "")), csv("last,"))
    }

    // 公式 CSV の実際の行（作品 059898 の著者と翻訳者の 2 行）
    @Suppress("ktlint:standard:max-line-length")
    private val sample =
        "作品ID,作品名,作品名読み,ソート用読み,副題,副題読み,原題,初出,分類番号,文字遣い種別,作品著作権フラグ,公開日,最終更新日,図書カードURL,人物ID,姓,名,姓読み,名読み,姓読みソート用,名読みソート用,姓ローマ字,名ローマ字,役割フラグ,生年月日,没年月日,人物著作権フラグ,底本名1,底本出版社名1,底本初版発行年1,入力に使用した版1,校正に使用した版1,底本の親本名1,底本の親本出版社名1,底本の親本初版発行年1,底本名2,底本出版社名2,底本初版発行年2,入力に使用した版2,校正に使用した版2,底本の親本名2,底本の親本出版社名2,底本の親本初版発行年2,入力者,校正者,テキストファイルURL,テキストファイル最終更新日,テキストファイル符号化方式,テキストファイル文字集合,テキストファイル修正回数,XHTML/HTMLファイルURL,XHTML/HTMLファイル最終更新日,XHTML/HTMLファイル符号化方式,XHTML/HTMLファイル文字集合,XHTML/HTMLファイル修正回数\r\n" +
            "\"059898\",\"ウェストミンスター寺院\",\"ウェストミンスターじいん\",\"うえすとみんすたあしいん\",\"\",\"\",\"\",\"\",\"NDC 933\",\"新字新仮名\",\"なし\",2020-04-03,2020-03-28,\"https://www.aozora.gr.jp/cards/001257/card59898.html\",\"001257\",\"アーヴィング\",\"ワシントン\",\"アーヴィング\",\"ワシントン\",\"ああういんく\",\"わしんとん\",\"Irving\",\"Washington\",\"著者\",\"1783-04-03\",\"1859-11-28\",\"なし\",\"スケッチ・ブック\",\"新潮文庫、新潮社\",\"1957（昭和32）年5月20日\",\"2000（平成12）年2月20日33刷改版\",\"2000（平成12）年2月20日33刷改版\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"えにしだ\",\"砂場清隆\",\"https://www.aozora.gr.jp/cards/001257/files/59898_ruby_70679.zip\",\"2020-03-28\",\"ShiftJIS\",\"JIS X 0208\",\"0\",\"https://www.aozora.gr.jp/cards/001257/files/59898_70731.html\",\"2020-03-28\",\"ShiftJIS\",\"JIS X 0208\",\"0\"\r\n" +
            "\"059898\",\"ウェストミンスター寺院\",\"ウェストミンスターじいん\",\"うえすとみんすたあしいん\",\"\",\"\",\"\",\"\",\"NDC 933\",\"新字新仮名\",\"なし\",2020-04-03,2020-03-28,\"https://www.aozora.gr.jp/cards/001257/card59898.html\",\"001258\",\"吉田\",\"甲子太郎\",\"よしだ\",\"きねたろう\",\"よした\",\"きねたろう\",\"Yoshida\",\"Kinetaro\",\"翻訳者\",\"1894-03-23\",\"1957-01-08\",\"なし\",\"スケッチ・ブック\",\"新潮文庫、新潮社\",\"1957（昭和32）年5月20日\",\"2000（平成12）年2月20日33刷改版\",\"2000（平成12）年2月20日33刷改版\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"えにしだ\",\"砂場清隆\",\"https://www.aozora.gr.jp/cards/001257/files/59898_ruby_70679.zip\",\"2020-03-28\",\"ShiftJIS\",\"JIS X 0208\",\"0\",\"https://www.aozora.gr.jp/cards/001257/files/59898_70731.html\",\"2020-03-28\",\"ShiftJIS\",\"JIS X 0208\",\"0\"\r\n"

    @Test
    fun parseCatalogSplitsRowsIntoWorksPersonsAndCredits() {
        val catalog = parseCatalog(StringReader("\uFEFF" + sample))

        val work = catalog.works.single()
        assertEquals(59898, work.id)
        assertEquals("ウェストミンスター寺院", work.title)
        assertEquals("NDC 933", work.ndc)
        assertEquals("2020-04-03", work.publishedOn)
        assertEquals("https://www.aozora.gr.jp/cards/001257/files/59898_70731.html", work.xhtmlUrl)
        assertEquals("スケッチ・ブック", work.source1?.name)
        assertNull(work.source2)

        assertEquals(listOf(1257, 1258), catalog.persons.map { it.id })
        assertEquals(
            listOf(Credit(59898, 1257, "著者"), Credit(59898, 1258, "翻訳者")),
            catalog.credits,
        )
    }

    @Test
    fun ndcTagsFollowTheOriginalApp() {
        assertEquals(listOf("詩歌", "記録・手記・ルポルタージュ"), ndcTags("NDC 911 916"))
        assertEquals(listOf("文学", "小説・物語"), ndcTags("NDC K913"))
        assertEquals(listOf("歴史", "伝記"), ndcTags("NDC 289"))
        assertEquals(listOf("文学", "日本文学"), ndcTags("NDC 910"))
        assertEquals(emptyList<String>(), ndcTags(""))
        assertEquals(emptyList<String>(), ndcTags("NDC "))
    }

    @Test
    fun kanaRangeCoversEachKanaUpToTheNextOne() {
        assertEquals(YomiRange("ぁ", "い"), kanaRange("あ"))
        assertEquals(YomiRange("か", "き"), kanaRange("か"))
        assertEquals(YomiRange("こ", "さ"), kanaRange("こ"))
        assertEquals(YomiRange("ん", "ゟ"), kanaRange("ん"))
        assertEquals(YomiRange("", ""), kanaRange("他"))
        // 格子の文字は空きマスを除いて 46 文字と「他」
        assertEquals(47, KANA_GRID.filterNotNull().size)
    }

    @Test
    fun searchKeyIgnoresKanaWidthCaseAndSpaces() {
        // 読みの片仮名は平仮名に、全角英数字は半角の小文字に、空白は除く
        assertEquals("うぇすとみんすたーじいん", searchKey("ウェストミンスターじいん"))
        assertEquals("abc12", searchKey("ＡＢＣ　１２"))
        assertEquals("芥川\nあくたがわ", searchKey("芥川", "あくたがわ"))
        // 打った文字も同じ形にそろえ、LIKE の記号はエスケープする
        assertEquals("うぇすと", searchLike("ウェスト"))
        assertEquals("100\\%\\_", searchLike("100%_"))
        // 姓名は続けて打っても引ける
        val person = parseCatalog(StringReader("\uFEFF" + sample)).persons.first()
        assert(person.searchKey.contains(searchKey("アーヴィングワシントン")))
        assert(person.searchKey.contains("washingtonirving"))
    }

    @Test
    fun rankingKeepsTheOfficialOrder() {
        // 公式の表のとおり 500 件で、重複はない。1 位は〔雨ニモマケズ〕、500 位は作品 105
        assertEquals(500, RANKING_2022.distinct().size)
        assertEquals(1, rankOf(45630))
        assertEquals(500, rankOf(105))
        assertNull(rankOf(59898))
    }

    @Test
    fun ndcNamesFollowEachLevel() {
        assertEquals("文学", ndcName("9"))
        assertEquals("日本文学", ndcName("91"))
        assertEquals("小説・物語", ndcName("913"))
        assertEquals("情報学・情報科学", ndcName("007"))
        // 番号のない区分（001）と、10 版で使われなくなった区分（119）は並べない
        assertEquals(listOf("000", "002", "007"), ndcChildren("00"))
        assertEquals(9, ndcChildren("11").size)
        assertEquals(10, ndcChildren("9").size)
    }

    @Test
    fun countNdcCountsEachWorkOncePerDivision() {
        val counts = countNdc(listOf("NDC 913", "NDC K913", "NDC 913 914", "NDC 756 K914", ""))
        assertEquals(4, counts.of("9", kidsOnly = false))
        assertEquals(3, counts.of("913", kidsOnly = false))
        assertEquals(2, counts.of("9", kidsOnly = true))
        assertEquals(1, counts.of("913", kidsOnly = true))
        assertEquals(1, counts.of("7", kidsOnly = false))
        assertEquals(0, counts.of("7", kidsOnly = true))
    }

    @Test
    fun aboutHtmlTakesTheAboutCellWithoutTheWikipediaLink() {
        // 図書カード（card789）の実際の行を短くしたもの。紹介文の後ろに Wikipedia へのリンクがつく
        val wikipedia =
            """<a href="http://ja.wikipedia.org/" target="_blank">""" +
                """<img align="middle" src="../images/wikipedia_logo_rounded.png" alt="wikipediaアイコン"></a>""" +
                """「<a href="http://ja.wikipedia.org/wiki/%E5%90%BE" target="_blank">吾輩は猫である</a>」"""
        val card = """<tr><td class="header">作品について：</td><td>明治38（1905）年1月、「ホトトギス」に発表。<br>$wikipedia</td></tr>
<tr><td class="header">文字遣い種別：</td><td>新字新仮名</td></tr>"""
        assertEquals("明治38（1905）年1月、「ホトトギス」に発表。<br>", aboutHtml(card, "作品について"))
        assertNull(aboutHtml("""<tr><td class="header">文字遣い種別：</td><td>新字新仮名</td></tr>""", "作品について"))
        // 作家ページも同じ形の欄を持つ。リンクしかない欄は空になる
        assertEquals("", aboutHtml("""<tr><td class="header">人物について：</td><td>$wikipedia</td></tr>""", "人物について"))
        assertEquals(
            "岩手県花巻に生まれる。",
            aboutHtml("""<tr><td class="header">人物について：</td><td>岩手県花巻に生まれる。</td></tr>""", "人物について"),
        )
    }
}
