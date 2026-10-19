package net.clagon.aozora.reader

import net.clagon.aozora.catalog.connect
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.util.UUID

/**
 * 本文の挿絵と外字の画像の置き場。リーダーが読み込む画像を、初めてのときは青空文庫から取ってきて dir に残し、
 * 2 回目からは残したものを返す。置き場所はサイトでのパスそのまま（例: cards/000148/files/789_14547_files/fig1.png）で、
 * 外字の画像（gaiji/…）は作品をまたいで使い回す。
 */
class Images(private val dir: File) {
    /** url の画像を開く。青空文庫の画像でないときや、取りに行けなかったときは null（WebView に任せる）。 */
    fun open(url: String): InputStream? {
        val file = fileFor(url) ?: return null
        if (!file.exists()) {
            val conn = connect(url)
            try {
                if (conn.responseCode != HttpURLConnection.HTTP_OK) return null
                // 書きかけのファイルを画像として返さないよう、別名で書いてから置き換える。
                // 同じ外字を同時に取りに行くこともあるので、別名は取りに行くごとに変える
                file.parentFile?.mkdirs()
                val part = File("${file.path}.${UUID.randomUUID()}.part")
                conn.inputStream.use { input -> part.outputStream().use { input.copyTo(it) } }
                part.renameTo(file)
            } catch (_: Exception) {
                return null
            } finally {
                conn.disconnect()
            }
        }
        return file.inputStream()
    }

    /** url を残す先。青空文庫のサイトの画像でなければ null。パスが dir の外を指すもの（`..` など）も null。 */
    internal fun fileFor(url: String): File? {
        val uri = runCatching { URI(url).normalize() }.getOrNull() ?: return null
        if (uri.scheme !in setOf("http", "https") || uri.host !in HOSTS) return null
        val path = uri.path?.takeIf { it.substringAfterLast('.').lowercase() in IMAGE_EXTENSIONS } ?: return null
        val file = File(dir, path.trimStart('/'))
        return file.takeIf { it.canonicalPath.startsWith(dir.canonicalPath + File.separator) }
    }

    private companion object {
        val HOSTS = setOf("www.aozora.gr.jp", "aozora.gr.jp")
        val IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "gif")
    }
}
