package net.clagon.aozora

import android.os.Bundle
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
