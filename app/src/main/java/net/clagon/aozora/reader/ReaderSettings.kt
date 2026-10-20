package net.clagon.aozora.reader

import android.content.Context
import androidx.core.content.edit

/** 余白・行間の 3 段階。 */
enum class Spacing(val label: String) {
    Narrow("狭い"),
    Normal("標準"),
    Wide("広い"),
}

/** 本文の配色。paper は地、ink は文字の色（ARGB）。dark なら外字の画像（黒い字）を反転して読めるようにする。 */
data class Palette(val paper: Long, val ink: Long, val dark: Boolean = false)

/** 本文の背景。背景パネルで選ぶ。 */
enum class Background(val label: String, val palette: Palette) {
    White("白", Palette(0xFFFFFFFF, 0xFF222222)),
    Black("黒", Palette(0xFF000000, 0xFFC8C8C8, dark = true)),
    Cream("生成り", Palette(0xFFF6F0E1, 0xFF3B3328)),
    Green("緑", Palette(0xFFE1EDD9, 0xFF26331F)),
    Blue("青", Palette(0xFFDCE7F2, 0xFF1F2A36)),
    Pink("ピンク", Palette(0xFFF7E1E4, 0xFF3A2427)),
}

/** 夜間の間だけ、背景の代わりに使う暗い配色。 */
val NIGHT = Palette(0xFF1B1B1D, 0xFFA8A8A8, dark = true)

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
    val background: Background = Background.White,
    /** 夜間。オンの間だけ背景を [NIGHT] で上書きし、オフで元の背景に戻る。 */
    val night: Boolean = false,
) {
    /** 今の本文の配色。 */
    val palette get() = if (night) NIGHT else background.palette

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
        val css = { argb: Long -> "#%06x".format(argb and 0xFFFFFF) }
        return ":root{--font-size:${fontSize}px;--line-height:$line;--margin-x:${marginX}px;--margin-y:${marginY}px;" +
            "--paper:${css(
                palette.paper,
            )};--ink:${css(palette.ink)};--gaiji-filter:${if (palette.dark) "invert(1)" else "none"}}"
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
                background = Background.entries.firstOrNull { it.name == prefs.getString("background", null) }
                    ?: default.background,
                night = prefs.getBoolean("night", default.night),
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
        putString("background", background.name)
        putBoolean("night", night)
    }
}
