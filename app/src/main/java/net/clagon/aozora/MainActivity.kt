package net.clagon.aozora

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import net.clagon.aozora.ui.App
import android.graphics.Color as AndroidColor

/** 唯一の Activity。システムバーの見た目とテーマを決め、画面はすべて [App] に任せる。 */
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
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
        )
        setContent {
            // ponytail: カラーモード（システム・ライト・ダーク。既定はシステム）の設定を作るまでは、ライトで固定する
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Accent,
                    background = Color.White,
                    surface = Color.White,
                    surfaceContainerHigh = Color(0xFFF0F2F4),
                    // 下から出すパネルなどの地は白にし、選んだ項目は青の淡い色で示す（既定の紫を使わない）
                    surfaceContainerLow = Color.White,
                    secondaryContainer = Color(0xFFE3EDFC),
                    onSecondaryContainer = Color(0xFF1F4E9A),
                ),
            ) {
                App()
            }
        }
    }
}

/** 選択中のタブやリンクに使う青。 */
private val Accent = Color(0xFF4A86E8)
