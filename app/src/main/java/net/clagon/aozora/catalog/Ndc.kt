package net.clagon.aozora.catalog

/** 分野。NDC の第 1 次区分（類）と、その下の第 2 次区分（綱）。 */
class NdcClass(val digit: Int, val name: String, val divisions: List<String>) {
    /** 第 2 次区分の 2 桁の番号（例: "91"）と名前の組 */
    val codes get() = divisions.mapIndexed { i, name -> "$digit$i" to name }
}

// NDC 新訂 10 版の第 2 次区分表
val NDC_CLASSES = listOf(
    NdcClass(0, "総記", listOf("総記", "図書館・図書館情報学", "図書・書誌学", "百科事典", "一般論文集・一般講演集", "逐次刊行物", "団体", "ジャーナリズム・新聞", "叢書・全集・選集", "貴重書・郷土資料・その他の特別コレクション")),
    NdcClass(1, "哲学", listOf("哲学", "哲学各論", "東洋思想", "西洋哲学", "心理学", "倫理学・道徳", "宗教", "神道", "仏教", "キリスト教")),
    NdcClass(2, "歴史", listOf("歴史", "日本史", "アジア史・東洋史", "ヨーロッパ史・西洋史", "アフリカ史", "北アメリカ史", "南アメリカ史", "オセアニア史・両極地方史", "伝記", "地理・地誌・紀行")),
    NdcClass(3, "社会科学", listOf("社会科学", "政治", "法律", "経済", "財政", "統計", "社会", "教育", "風俗習慣・民俗学・民族学", "国防・軍事")),
    NdcClass(4, "自然科学", listOf("自然科学", "数学", "物理学", "化学", "天文学・宇宙科学", "地球科学・地学", "生物科学・一般生物学", "植物学", "動物学", "医学・薬学")),
    NdcClass(5, "技術・工学", listOf("技術・工学", "建設工学・土木工学", "建築学", "機械工学・原子力工学", "電気工学", "海洋工学・船舶工学・兵器", "金属工学・鉱山工学", "化学工業", "製造工業", "家政学・生活科学")),
    NdcClass(6, "産業", listOf("産業", "農業", "園芸", "蚕糸業", "畜産業・獣医学", "林業", "水産業", "商業", "運輸・交通", "通信事業")),
    NdcClass(7, "芸術・美術", listOf("芸術・美術", "彫刻", "絵画・書道", "版画", "写真", "工芸", "音楽・舞踊", "演劇・映画", "スポーツ・体育", "諸芸・娯楽")),
    NdcClass(8, "言語", listOf("言語", "日本語", "中国語・その他の東洋の言語", "英語", "ドイツ語", "フランス語", "スペイン語", "イタリア語", "ロシア語", "その他の諸言語")),
    NdcClass(9, "文学", listOf("文学", "日本文学", "中国文学・その他の東洋文学", "英米文学", "ドイツ文学", "フランス文学", "スペイン文学", "イタリア文学", "ロシア・ソビエト文学", "その他の諸文学")),
)

// 文学（9xx）の 3 桁目が表す文学形式。0 と 9 は形式ではないので null
private val LITERARY_FORMS = listOf(null, "詩歌", "戯曲", "小説・物語", "評論・エッセイ・随筆", "日記・書簡・紀行", "記録・手記・ルポルタージュ", "箴言・アフォリズム・寸言", "作品集", null)

/**
 * 作品カードに出すタグ。元アプリに合わせ、分類ごとに文学なら形式名、それ以外は第 2 次区分の名前を出す。
 * 分類が 1 つだけのときは、先頭に第 1 次区分の名前（例: 文学）を足す。
 */
fun ndcTags(ndc: String): List<String> {
    val codes = ndc.removePrefix("NDC").split(' ').map { it.removePrefix("K") }
        .filter { it.length == 3 && it.all(Char::isDigit) }
        .map { code -> code.map(Char::digitToInt) }
    val tags = codes.map { (c, d, f) -> (if (c == 9) LITERARY_FORMS[f] else null) ?: NDC_CLASSES[c].divisions[d] }.distinct()
    return if (codes.size == 1) listOf(NDC_CLASSES[codes[0][0]].name) + tags else tags
}

/**
 * 読み（ソート用の値。平仮名・濁点なし）の範囲。from 以上 to 未満を文字コード順で比べる。
 * from が空のときは、平仮名で始まらない読み（記号・英字・片仮名など）を表す。
 */
data class YomiRange(val from: String, val to: String)

private const val HIRAGANA_END = "ゟ" // 平仮名ブロックの末尾。これ以上は片仮名など
private val OTHER_YOMI = YomiRange("", "")

/** 作家別の縦タブに並べる五十音の行。 */
enum class KanaRow(val label: String, val range: YomiRange) {
    A("あ行", YomiRange("ぁ", "か")),
    KA("か行", YomiRange("か", "さ")),
    SA("さ行", YomiRange("さ", "た")),
    TA("た行", YomiRange("た", "な")),
    NA("な行", YomiRange("な", "は")),
    HA("は行", YomiRange("は", "ま")),
    MA("ま行", YomiRange("ま", "や")),
    YA("や行", YomiRange("や", "ら")),
    RA("ら行", YomiRange("ら", "わ")),
    WA("わ行", YomiRange("わ", HIRAGANA_END)),
    OTHER("その他", OTHER_YOMI),
}

private const val KANA = "あいうえおかきくけこさしすせそたちつてとなにぬねのはひふへほまみむめもやゆよらりるれろわをん"

/** 作品別の格子に 5 列で並べる文字。null は空きマス（や行・わ行の欠け）、「他」は平仮名以外。 */
val KANA_GRID: List<String?> = "あいうえおかきくけこさしすせそたちつてとなにぬねのはひふへほまみむめもや_ゆ_よらりるれろわ_を_ん他"
    .map { if (it == '_') null else it.toString() }

/** その文字で始まる読みの範囲。小書きの仮名などは直前の文字に含める（ソート用の読みにはほぼ出てこない）。 */
fun kanaRange(kana: String): YomiRange {
    val i = KANA.indexOf(kana)
    if (i < 0) return OTHER_YOMI
    val from = if (i == 0) "ぁ" else kana
    val to = if (i == KANA.lastIndex) HIRAGANA_END else KANA[i + 1].toString()
    return YomiRange(from, to)
}
