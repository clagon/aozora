package net.clagon.aozora.ui.other

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.clagon.aozora.library.DayReading
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * 直近 7 日の読書時間の折れ線。点をなめらかな曲線でつなぎ、下を薄く塗る。
 * 各点の上に時間（`4m`）、下に曜日（昨日は「昨」、今日は「今」）を出す。
 */
@Composable
fun ReadingTimeChart(days: List<DayReading>, modifier: Modifier = Modifier) {
    val today = days.lastOrNull()?.day ?: return
    val color = MaterialTheme.colorScheme.primary
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline)
    val measurer = rememberTextMeasurer()
    // 全部 0 の日でも線が下に寝るよう、1 分を最小の高さにする
    val max = maxOf(days.maxOf { it.millis }, 60_000L).toFloat()
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(120.dp)) {
            val step = size.width / days.size
            val top = 24.dp.toPx()
            val bottom = size.height - 4.dp.toPx()
            val points = days.mapIndexed { i, d ->
                Offset(step * (i + 0.5f), bottom - (bottom - top) * d.millis / max)
            }
            val line = Path().apply {
                moveTo(points[0].x, points[0].y)
                points.zipWithNext { a, b ->
                    val mid = (a.x + b.x) / 2
                    cubicTo(mid, a.y, mid, b.y, b.x, b.y)
                }
            }
            val fill = Path().apply {
                addPath(line)
                lineTo(points.last().x, size.height)
                lineTo(points[0].x, size.height)
                close()
            }
            drawPath(fill, color.copy(alpha = 0.15f))
            drawPath(line, color, style = Stroke(2.dp.toPx()))
            points.forEachIndexed { i, p ->
                drawCircle(color, 3.dp.toPx(), p)
                val label = measurer.measure(minutesLabel(days[i].millis), labelStyle)
                drawText(label, topLeft = Offset(p.x - label.size.width / 2, p.y - label.size.height - 4.dp.toPx()))
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            days.forEach {
                Text(
                    dayLabel(it.day, today),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private val WEEKDAYS = mapOf(
    DayOfWeek.MONDAY to "月",
    DayOfWeek.TUESDAY to "火",
    DayOfWeek.WEDNESDAY to "水",
    DayOfWeek.THURSDAY to "木",
    DayOfWeek.FRIDAY to "金",
    DayOfWeek.SATURDAY to "土",
    DayOfWeek.SUNDAY to "日",
)

/** 横軸の 1 文字。今日は「今」、昨日は「昨」、それより前は曜日。 */
internal fun dayLabel(day: LocalDate, today: LocalDate): String = when (day) {
    today -> "今"
    today.minusDays(1) -> "昨"
    else -> WEEKDAYS.getValue(day.dayOfWeek)
}

/** 点に添える時間。分で出し、1 時間を超えたら時間と分にする（`4m`、`1h05m`）。 */
internal fun minutesLabel(millis: Long): String {
    val minutes = millis / 60_000
    return if (minutes < 60) "${minutes}m" else "%dh%02dm".format(minutes / 60, minutes % 60)
}
