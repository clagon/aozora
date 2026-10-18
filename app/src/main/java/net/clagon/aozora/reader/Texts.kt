package net.clagon.aozora.reader

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.clagon.aozora.catalog.Work
import net.clagon.aozora.catalog.connect
import java.io.File
import java.nio.charset.Charset

/**
 * 作品の本文（XHTML）の置き場。初めて開いたときに青空文庫から取ってきて dir に UTF-8 で残し、
 * 2 回目からは残したものを使う。本棚に入れたかどうかとは関係なく残す。
 */
// ponytail: 挿絵と外字の画像はまだ残さず、読むたびにサイトから読み込む。オフラインで読むときに残す
class Texts(private val dir: File) {
    /** 本文を返す。取りに行けなければ例外を投げる（画面が理由を出す）。 */
    suspend fun load(work: Work): String = withContext(Dispatchers.IO) {
        val file = File(dir, "${work.id}.html")
        if (file.exists()) return@withContext file.readText()
        require(work.xhtmlUrl.isNotEmpty()) { "この作品には XHTML 版がありません" }
        val conn = connect(work.xhtmlUrl)
        val text = try {
            decodeXhtml(conn.inputStream.use { it.readBytes() })
        } finally {
            conn.disconnect()
        }
        // 書きかけのファイルを本文として読まないよう、別名で書いてから置き換える
        dir.mkdirs()
        val part = File(dir, "${work.id}.html.part")
        part.writeText(text)
        part.renameTo(file)
        text
    }
}

/**
 * XHTML のバイト列を文字列にする。文字コードは先頭の XML 宣言で決まり、ほとんどは Shift_JIS、新しい作品は UTF-8。
 * Shift_JIS は、「～」など Windows の文字も読めるよう windows-31j として読む。
 */
internal fun decodeXhtml(bytes: ByteArray): String {
    val head = String(bytes, 0, minOf(bytes.size, 200), Charsets.ISO_8859_1)
    val declared = Regex("""encoding="([^"]+)"""").find(head)?.groupValues?.get(1)
    val charset = if (declared == null || declared.equals("Shift_JIS", ignoreCase = true)) "windows-31j" else declared
    return String(bytes, Charset.forName(charset))
}

/**
 * 本文の XHTML を、リーダーで表示する HTML にする。サイトの CSS とスクリプトは外し、
 * 縦書きと段組みの CSS（[css]）、文字パネルの設定の CSS の変数（[variables]）、ページをめくるスクリプト（[script]）を差し込む。
 * 最初から設定どおりに組むので、開いた直後に組み直して見た目が変わることがない。
 */
internal fun readerHtml(xhtml: String, css: String, variables: String, script: String): String = xhtml
    .replace(Regex("""<link [^>]*rel="stylesheet"[^>]*>"""), "")
    .replace(Regex("""<script [^>]*src="[^"]*"[^>]*>\s*</script>"""), "")
    .replaceFirst(
        "</head>",
        "<style>$css</style><style id=\"reader-vars\">$variables</style><script>$script</script></head>",
    )
