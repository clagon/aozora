package net.clagon.aozora.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import net.clagon.aozora.reader.Background
import net.clagon.aozora.reader.ReaderSettings

/**
 * 背景パネル。本文の地の色を 6 色から選ぶと、すぐ本文に反映する（onChange）。
 * 夜間がオンの間は夜間の配色が優先されるので、選んだ背景は夜間を切ったときに見えることを添える。
 */
// ponytail: 輝度の調整と画像の背景テーマは作らない（docs/spec.md の後回し）
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackgroundPanel(settings: ReaderSettings, onChange: (ReaderSettings) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, scrimColor = Color.Transparent) {
        StatusBarIconsFor(settings)
        Column(Modifier.navigationBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 24.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Background.entries.forEach {
                    Swatch(it, it == settings.background) { onChange(settings.copy(background = it)) }
                }
            }
            if (settings.night) {
                Text(
                    "夜間がオンの間は、夜間の配色で表示します",
                    color = MaterialTheme.colorScheme.outline,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }
    }
}

/** 背景の 1 色。丸に地の色を塗り、中に文字の色で「あ」を置く。選んでいる色には青い縁とチェックをつける。 */
@Composable
private fun Swatch(background: Background, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.selectable(selected, role = Role.RadioButton, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val edge = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(Color(background.palette.paper))
                .border(if (selected) 2.dp else 1.dp, edge, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            } else {
                Text("あ", color = Color(background.palette.ink), style = MaterialTheme.typography.titleMedium)
            }
        }
        Text(
            background.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
