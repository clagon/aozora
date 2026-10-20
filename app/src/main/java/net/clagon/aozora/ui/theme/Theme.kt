package net.clagon.aozora.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.core.content.edit

/** アプリ全体の配色。リーダーの夜間とは別のもの。 */
enum class ColorMode(val label: String) {
    System("システム"),
    Light("ライト"),
    Dark("ダーク"),
    ;

    /** この配色で暗くするか。システムなら端末の設定に従う。 */
    @Composable
    fun isDark() = when (this) {
        System -> isSystemInDarkTheme()
        Light -> false
        Dark -> true
    }

    fun save(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
        putString("colorMode", name)
    }

    companion object {
        private const val PREFS = "app_settings"

        /** 残した配色を読む。知らない値なら既定（システム）。 */
        fun load(context: Context): ColorMode {
            val name = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("colorMode", null)
            return entries.firstOrNull { it.name == name } ?: System
        }
    }
}

/** 選択中のタブやリンクに使う青。 */
private val Accent = Color(0xFF4A86E8)

// 下から出すパネルなどの地は白（暗いときは地と同じ黒）にし、選んだ項目は青の淡い色で示す（既定の紫を使わない）
private val LightColors = lightColorScheme(
    primary = Accent,
    background = Color.White,
    surface = Color.White,
    surfaceContainerHigh = Color(0xFFF0F2F4),
    surfaceContainerLow = Color.White,
    secondaryContainer = Color(0xFFE3EDFC),
    onSecondaryContainer = Color(0xFF1F4E9A),
)

private val DarkColors = darkColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    background = Color(0xFF121212),
    surface = Color(0xFF121212),
    surfaceContainerHigh = Color(0xFF2A2C2F),
    surfaceContainerLow = Color(0xFF1E1F21),
    secondaryContainer = Color(0xFF1F3A63),
    onSecondaryContainer = Color(0xFFD3E3FD),
)

/** アプリのテーマ。dark なら暗い配色にする。色を指定しない文字も地に合うよう、既定の文字色を onBackground にする。 */
@Composable
fun AozoraTheme(dark: Boolean, content: @Composable () -> Unit) {
    val colors = if (dark) DarkColors else LightColors
    MaterialTheme(colorScheme = colors) {
        CompositionLocalProvider(LocalContentColor provides colors.onBackground, content = content)
    }
}
