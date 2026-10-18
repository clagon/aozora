package net.clagon.aozora.ui.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.clagon.aozora.reader.MAX_FONT_SIZE
import net.clagon.aozora.reader.MIN_FONT_SIZE
import net.clagon.aozora.reader.ReaderSettings
import net.clagon.aozora.reader.Spacing

/**
 * 文字パネル。文字サイズ・余白・行間・ページめくりを変えると、すぐ本文に反映する（onChange）。
 * 変わる様子が見えるよう、後ろの本文は暗くしない。
 */
// ponytail: フォントの切り替えは、同梱するフォントを決めてから足す
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextPanel(settings: ReaderSettings, onChange: (ReaderSettings) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, scrimColor = Color.Transparent) {
        Column(
            Modifier.navigationBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Setting("文字サイズ") {
                OutlinedIconButton(
                    onClick = { onChange(settings.copy(fontSize = settings.fontSize - 1)) },
                    enabled = settings.fontSize > MIN_FONT_SIZE,
                ) { Icon(Icons.Filled.Remove, contentDescription = "小さく") }
                Text(
                    settings.fontSize.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                OutlinedIconButton(
                    onClick = { onChange(settings.copy(fontSize = settings.fontSize + 1)) },
                    enabled = settings.fontSize < MAX_FONT_SIZE,
                ) { Icon(Icons.Filled.Add, contentDescription = "大きく") }
            }
            Setting("ページめくり") {
                Choices(listOf(true to "スライド", false to "なし"), settings.slide) { onChange(settings.copy(slide = it)) }
            }
            Setting("余白") {
                Choices(
                    Spacing.entries.map {
                        it to it.label
                    },
                    settings.margin,
                ) { onChange(settings.copy(margin = it)) }
            }
            Setting("行間") {
                Choices(Spacing.entries.map { it to it.label }, settings.lineHeight) {
                    onChange(settings.copy(lineHeight = it))
                }
            }
        }
    }
}

/** 項目名と、その右に並べる操作の 1 行。 */
@Composable
private fun Setting(label: String, content: @Composable RowScope.() -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            color = MaterialTheme.colorScheme.outline,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(96.dp),
        )
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, content = content)
    }
}

/** 選択肢をつなげたボタン。押した選択肢の値を onSelect に渡す。 */
@Composable
private fun <T> RowScope.Choices(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.weight(1f)) {
        options.forEachIndexed { i, (value, label) ->
            SegmentedButton(
                selected = value == selected,
                onClick = { onSelect(value) },
                shape = SegmentedButtonDefaults.itemShape(i, options.size),
                icon = {},
            ) { Text(label) }
        }
    }
}
