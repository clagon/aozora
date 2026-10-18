package net.clagon.aozora.catalog

import java.io.Reader
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val CATALOG_URL = "https://www.aozora.gr.jp/index_pages/list_person_all_extended_utf8.zip"

sealed interface CatalogState {
    data object Ready : CatalogState
    data object Updating : CatalogState
    data class Failed(val message: String) : CatalogState
}

/**
 * カタログの取り込みと更新。初回起動時に取り込み、以後は利用者の操作でだけ取り直す。
 * 取り込みはアプリ全体のスコープで走らせ、画面を離れても中断されないようにしている。
 */
class Catalog(private val dao: CatalogDao, private val scope: CoroutineScope) {
    private val _state = MutableStateFlow<CatalogState>(CatalogState.Ready)
    val state = _state.asStateFlow()
    val workCount = dao.workCount()

    fun updateIfEmpty() {
        scope.launch { if (dao.workCount().first() == 0) update() }
    }

    /** メインスレッドから呼ぶ。取り込み中なら何もしない。 */
    fun update() {
        if (_state.value == CatalogState.Updating) return
        _state.value = CatalogState.Updating
        scope.launch {
            _state.value = try {
                dao.replaceAll(withContext(Dispatchers.IO) { download() })
                CatalogState.Ready
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                CatalogState.Failed(e.message ?: e.javaClass.simpleName)
            }
        }
    }

    // ponytail: 全件をメモリに載せてから DB に入れる（作品 1.8 万件で数十 MB）。重くなったら行ごとに insert する
    private fun download(): ParsedCatalog {
        val conn = URL(CATALOG_URL).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 30_000
        try {
            return ZipInputStream(conn.inputStream.buffered()).use { zip ->
                generateSequence { zip.nextEntry }.firstOrNull { it.name.endsWith(".csv") }
                    ?: error("zip の中に CSV がありません")
                parseCatalog(zip.reader())
            }
        } finally {
            conn.disconnect()
        }
    }
}

class ParsedCatalog(val works: List<Work>, val persons: List<Person>, val credits: List<Credit>)

/** 公式 CSV（1 行が作品と作家の組）を、作品・作家・その組に分ける。列は見出し名で引く。 */
internal fun parseCatalog(reader: Reader): ParsedCatalog {
    val rows = readCsv(reader).iterator()
    val index = rows.next().withIndex().associate { (i, name) -> name to i }
    val works = LinkedHashMap<Int, Work>()
    val persons = LinkedHashMap<Int, Person>()
    val credits = LinkedHashSet<Credit>()
    for (row in rows) {
        fun col(name: String) = row[index.getValue(name)]
        fun source(n: Int) = col("底本名$n").takeIf { it.isNotEmpty() }?.let {
            SourceBook(
                name = it,
                publisher = col("底本出版社名$n"),
                firstEdition = col("底本初版発行年$n"),
                inputEdition = col("入力に使用した版$n"),
                proofEdition = col("校正に使用した版$n"),
                parentName = col("底本の親本名$n"),
                parentPublisher = col("底本の親本出版社名$n"),
                parentFirstEdition = col("底本の親本初版発行年$n"),
            )
        }
        val workId = col("作品ID").toInt()
        val personId = col("人物ID").toInt()
        works.getOrPut(workId) {
            Work(
                id = workId,
                title = col("作品名"),
                titleYomi = col("作品名読み"),
                sortYomi = col("ソート用読み"),
                subtitle = col("副題"),
                subtitleYomi = col("副題読み"),
                originalTitle = col("原題"),
                firstAppearance = col("初出"),
                ndc = col("分類番号"),
                kanaType = col("文字遣い種別"),
                copyrighted = col("作品著作権フラグ") == "あり",
                publishedOn = col("公開日"),
                updatedOn = col("最終更新日"),
                cardUrl = col("図書カードURL"),
                textUrl = col("テキストファイルURL"),
                xhtmlUrl = col("XHTML/HTMLファイルURL"),
                inputter = col("入力者"),
                proofreader = col("校正者"),
                source1 = source(1),
                source2 = source(2),
            )
        }
        persons.getOrPut(personId) {
            Person(
                id = personId,
                lastName = col("姓"),
                firstName = col("名"),
                lastNameYomi = col("姓読み"),
                firstNameYomi = col("名読み"),
                lastNameSort = col("姓読みソート用"),
                firstNameSort = col("名読みソート用"),
                lastNameRoman = col("姓ローマ字"),
                firstNameRoman = col("名ローマ字"),
                born = col("生年月日"),
                died = col("没年月日"),
                copyrighted = col("人物著作権フラグ") == "あり",
            )
        }
        credits += Credit(workId, personId, col("役割フラグ"))
    }
    return ParsedCatalog(works.values.toList(), persons.values.toList(), credits.toList())
}

/** RFC 4180 の CSV を 1 行ずつ読む。`"` で囲んだ中のカンマ・改行・`""` を扱い、先頭の BOM は読み飛ばす。 */
internal fun readCsv(reader: Reader): Sequence<List<String>> = sequence {
    val r = reader.buffered()
    val row = ArrayList<String>()
    val field = StringBuilder()
    var inQuotes = false
    var c = r.read()
    if (c == 0xFEFF) c = r.read()
    while (c != -1) {
        val ch = c.toChar()
        if (inQuotes) {
            if (ch == '"') {
                val next = r.read()
                if (next != '"'.code) {
                    inQuotes = false
                    c = next
                    continue
                }
            }
            field.append(ch)
        } else when (ch) {
            '"' -> inQuotes = true
            ',' -> { row += field.toString(); field.clear() }
            '\r' -> {}
            '\n' -> { row += field.toString(); field.clear(); yield(row.toList()); row.clear() }
            else -> field.append(ch)
        }
        c = r.read()
    }
    if (field.isNotEmpty() || row.isNotEmpty()) {
        row += field.toString()
        yield(row.toList())
    }
}
