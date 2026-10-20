package net.clagon.aozora

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import net.clagon.aozora.ui.App
import net.clagon.aozora.ui.theme.AozoraTheme
import android.graphics.Color as AndroidColor

/** 唯一の Activity。システムバーの見た目とテーマ（カラーモード）を決め、画面はすべて [App] に任せる。 */
class MainActivity : ComponentActivity() {
    /**
     * 音量キーを受ける先。リーダーが音量キーでめくる設定のときだけ入れる（ReaderScreen）。
     * 引数は次へなら 1、前へなら -1。入っている間は音量を変えない。
     */
    var volumeKeys: ((Int) -> Unit)? = null

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val turn = volumeKeys ?: return super.dispatchKeyEvent(event)
        val delta = when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> 1
            KeyEvent.KEYCODE_VOLUME_UP -> -1
            else -> return super.dispatchKeyEvent(event)
        }
        // 押したときに 1 ページめくる（押し続けても繰り返さない）。離したときも含めて音量の変更には回さない
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) turn(delta)
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val colorMode = (application as AozoraApp).colorMode
        setContent {
            val dark = colorMode.collectAsState().value.isDark()
            // システムバーは透かし、アイコンの色を配色に合わせる
            LaunchedEffect(dark) {
                val style = if (dark) {
                    SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                } else {
                    SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            AozoraTheme(dark) { App() }
        }
    }
}
