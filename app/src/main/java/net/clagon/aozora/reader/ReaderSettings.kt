package net.clagon.aozora.reader

import android.content.Context
import androidx.core.content.edit

/** 余白・行間の 3 段階。 */
enum class Spacing(val label: String) {
    Narrow("狭い"),
    Normal("標準"),
    Wide("広い"),
}

/** 文字サイズの下限・上限（CSS の px。WebView では dp と同じ大きさ）。 */
const val MIN_FONT_SIZE = 12
const val MAX_FONT_SIZE = 32

/** リーダーの設定。文字パネルで変えるものと、その他タブで変えるもの（keepScreenOn・volumeKeys）。全作品で共通。 */
data class ReaderSettings(
    val fontSize: Int = 18,
    val margin: Spacing = Spacing.Normal,
    val lineHeight: Spacing = Spacing.Normal,
    /** めくるときに前のページを滑らせるか。false ならすぐ切り替える。 */
    val slide: Boolean = true,
    /** 読んでいる間は画面を消灯しない。 */
    val keepScreenOn: Boolean = false,
    /** 音量キーでページをめくる（下げるキーで次、上げるキーで前）。 */
    val volumeKeys: Boolean = false,
) {
    /** 本文の CSS の変数（assets/reader/reader.css）に入れる値。 */
    fun cssVariables(): String {
        val (marginX, marginY) = when (margin) {
            Spacing.Narrow -> 12 to 24
            Spacing.Normal -> 24 to 40
            Spacing.Wide -> 40 to 64
        }
        val line = when (lineHeight) {
            Spacing.Narrow -> 1.5
            Spacing.Normal -> 1.8
            Spacing.Wide -> 2.1
        }
        return ":root{--font-size:${fontSize}px;--line-height:$line;--margin-x:${marginX}px;--margin-y:${marginY}px}"
    }

    companion object {
        private const val PREFS = "reader_settings"

        /** 残した設定を読む。知らない値は既定に戻す。 */
        fun load(context: Context): ReaderSettings {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val default = ReaderSettings()
            fun spacing(key: String, fallback: Spacing) =
                Spacing.entries.firstOrNull { it.name == prefs.getString(key, null) } ?: fallback
            return ReaderSettings(
                fontSize = prefs.getInt("fontSize", default.fontSize).coerceIn(MIN_FONT_SIZE, MAX_FONT_SIZE),
                margin = spacing("margin", default.margin),
                lineHeight = spacing("lineHeight", default.lineHeight),
                slide = prefs.getBoolean("slide", default.slide),
                keepScreenOn = prefs.getBoolean("keepScreenOn", default.keepScreenOn),
                volumeKeys = prefs.getBoolean("volumeKeys", default.volumeKeys),
            )
        }
    }

    /** 設定を残す。 */
    fun save(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
        putInt("fontSize", fontSize)
        putString("margin", margin.name)
        putString("lineHeight", lineHeight.name)
        putBoolean("slide", slide)
        putBoolean("keepScreenOn", keepScreenOn)
        putBoolean("volumeKeys", volumeKeys)
    }
}
