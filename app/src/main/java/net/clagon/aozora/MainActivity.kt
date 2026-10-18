package net.clagon.aozora

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import net.clagon.aozora.ui.App

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
        )
        setContent {
            // ponytail: カラーモード（システム・ライト・ダーク。既定はシステム）の設定を作るまでは、ライトで固定する
            MaterialTheme(colorScheme = lightColorScheme(primary = Accent, background = Color.White, surface = Color.White, surfaceContainerHigh = Color(0xFFF0F2F4))) {
                App()
            }
        }
    }
}

private val Accent = Color(0xFF4A86E8)
