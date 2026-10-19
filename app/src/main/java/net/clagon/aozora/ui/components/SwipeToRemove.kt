package net.clagon.aozora.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** 幅のこの割合より左へ動かして離したら、消す。 */
private const val REMOVE_FRACTION = 0.4f

/**
 * 左へスワイプすると onRemove を呼ぶ行。動かしている間は、下から赤地にごみ箱のアイコンが見える。
 * 右へのスワイプは受けずに外側へ渡すので、タブを左右のスワイプで切り替える画面の中でも使える
 * （SwipeToDismissBox は使わない方向のドラッグも受けてしまう）。
 */
@Composable
fun SwipeToRemove(onRemove: () -> Unit, content: @Composable () -> Unit) {
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    Box {
        Box(
            Modifier.matchParentSize().background(MaterialTheme.colorScheme.error).padding(end = 24.dp),
            contentAlignment = Alignment.CenterEnd,
        ) { Icon(Icons.Filled.Delete, contentDescription = "削除", tint = Color.White) }
        Box(
            Modifier.offset { IntOffset(offset.value.roundToInt(), 0) }
                .background(MaterialTheme.colorScheme.surface)
                .pointerInput(Unit) {
                    val width = size.width.toFloat()
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        // 左へ動かしたときだけ受ける。右なら消費せず、外側（タブのページ送り）が受ける
                        val drag = awaitHorizontalTouchSlopOrCancellation(down.id) { change, over ->
                            if (over < 0) change.consume()
                        } ?: return@awaitEachGesture
                        var x = offset.value
                        horizontalDrag(drag.id) { change ->
                            x = (x + change.positionChange().x).coerceIn(-width, 0f)
                            scope.launch { offset.snapTo(x) }
                            change.consume()
                        }
                        scope.launch {
                            if (x < -width * REMOVE_FRACTION) {
                                offset.animateTo(-width)
                                onRemove()
                            } else {
                                offset.animateTo(0f)
                            }
                        }
                    }
                },
        ) { content() }
    }
}
