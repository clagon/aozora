package net.clagon.aozora.catalog

import android.text.Html
import java.io.Reader
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 青空文庫公式の全作品・全作家の一覧（拡張版・UTF-8）。zip の中に CSV が 1 つ入っている。 */
private const val CATALOG_URL = "https://www.aozora.gr.jp/index_pages/list_person_all_extended_utf8.zip"

/** カタログの取り込みの状態。画面はこれを見て、取り込み中の表示や失敗の理由を出す。 */
sealed interface CatalogState {
    /** 取り込み中ではない。カタログが空のこともある（件数は [Catalog.workCount] で見る）。 */
    data object Ready : CatalogState
    /** ダウンロードまたは DB への書き込みの途中。 */
    data object Updating : CatalogState
    /** 直前の取り込みが失敗した。message は利用者にそのまま見せる理由。前のカタログは残っている。 */
    data class Failed(val message: String) : CatalogState
}

/**
 * カタログの取り込みと更新。初回起動時に取り込み、以後は利用者の操作でだけ取り直す。
 * 取り込みはアプリ全体のスコープで走らせ、画面を離れても中断されないようにしている。
 */
class Catalog(val dao: CatalogDao, private val scope: CoroutineScope) {
    private val _state = MutableStateFlow<CatalogState>(CatalogState.Ready)
    /** 取り込みの状態。CatalogStatus とホームが購読する。 */
    val state = _state.asStateFlow()
    /** DB を読めないときは件数を出さない（読めない理由は update() 側で Failed として見せる）。 */
    val workCount = dao.workCount().catch {}
    /**
     * 分野ごとの件数。起動時から数え始めておき、探すタブを開いた時点でそろっているようにする
     * （数え終わる前のカードを出すと、作品のない分野が押せる見た目で一瞬出てしまう）。数え終わるまでは null。
     */
    val ndcCounts = dao.allNdc().map(::countNdc).flowOn(Dispatchers.Default).catch {}.stateIn(scope, SharingStarted.Eagerly, null)

    /** 起動時に AozoraApp から呼ぶ。カタログが空のときだけ取り込む（2 回目以降の起動では何もしない）。 */
    fun updateIfEmpty() = update(onlyIfEmpty = true)

    /** メインスレッドから呼ぶ。取り込み中なら何もしない。DB の読み書きはすべて下の try の中で行う。 */
    fun update(onlyIfEmpty: Boolean = false) {
        if (_state.value == CatalogState.Updating) return
        _state.value = CatalogState.Updating
        scope.launch {
            _state.value = try {
                if (!onlyIfEmpty || dao.workCount().first() == 0) {
                    dao.replaceAll(withContext(Dispatchers.IO) { download() })
                }
                CatalogState.Ready
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                CatalogState.Failed(e.message ?: e.javaClass.simpleName)
            }
        }
    }

    /**
     * 作品の「作品について」。残してあればそれを返し、なければ図書カードから取り出して残す。
     * 図書カードに書かれていなければ空文字、取りに行けなければ null（次に開いたときに取りに行き直す）。
     */
    suspend fun about(work: Work): String? {
        dao.about(work.id)?.let { return it }
        val about = try {
            withContext(Dispatchers.IO) {
                val conn = connect(work.cardUrl)
                try {
                    conn.inputStream.reader().use { it.readText() }
                } finally {
                    conn.disconnect()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return null
        }
        // Wikipedia のロゴ画像は Html.fromHtml で置換文字になるので先に外す
        val text = aboutHtml(about)?.replace(Regex("<img[^>]*>"), "")
            ?.let { Html.fromHtml(it, Html.FROM_HTML_MODE_COMPACT).toString().trim() }.orEmpty()
        dao.saveNote(WorkNote(work.id, text))
        return text
    }

    // ponytail: 全件をメモリに載せてから DB に入れる（作品 1.8 万件で数十 MB）。重くなったら行ごとに insert する
    private fun download(): ParsedCatalog {
        val conn = connect(CATALOG_URL)
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

/** 青空文庫のサイトへの接続。サイトが応答しないときに待ち続けないよう、時間の上限をつける。 */
private fun connect(url: String) = (URL(url).openConnection() as HttpURLConnection).apply {
    connectTimeout = 15_000
    readTimeout = 30_000
}

/** 図書カードの HTML から「作品について」の欄の中身（HTML のまま）を取り出す。欄がなければ null。 */
internal fun aboutHtml(card: String): String? =
    Regex("""作品について：</td><td>(.*?)</td>""", RegexOption.DOT_MATCHES_ALL).find(card)?.groupValues?.get(1)

/** CSV から読み取ったカタログ全体。[CatalogDao.replaceAll] でそのまま DB に入れる。 */
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
