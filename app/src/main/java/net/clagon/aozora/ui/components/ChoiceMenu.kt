package net.clagon.aozora.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 「2026 年 ▼」のように今の選択を太字で出し、押すと choices から選ぶメニューを開く。一覧の先頭の絞り込みに使う。
 * label は選択肢の見せ方、description は ▼ の読み上げ。
 */
@Composable
fun <T> ChoiceMenu(selected: T, choices: List<T>, label: (T) -> String, description: String, select: (T) -> Unit) {
    var shown by remember { mutableStateOf(false) }
    Box {
        Row(Modifier.clickable { shown = true }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label(selected), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Icon(Icons.Filled.ArrowDropDown, contentDescription = description)
        }
        DropdownMenu(
            shown,
            onDismissRequest = { shown = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            choices.forEach {
                DropdownMenuItem(
                    text = { Text(label(it)) },
                    onClick = {
                        select(it)
                        shown = false
                    },
                    trailingIcon = { if (it == selected) Icon(Icons.Filled.Check, contentDescription = "選択中") },
                )
            }
        }
    }
}
