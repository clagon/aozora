package net.clagon.aozora.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.clagon.aozora.catalog.WorkItem
import kotlin.random.Random

/** 表紙の色。文字色と帯の色の組。作品 ID で決めるので、同じ作品はいつも同じ色になる。 */
private val COVER_COLORS = listOf(
    Color(0xFFD9534F) to Color(0xFFE8897D),
    Color(0xFFC77DB5) to Color(0xFFDBA5D0),
    Color(0xFF4FAE5A) to Color(0xFF8AC98E),
    Color(0xFF8C7F74) to Color(0xFFB5A99E),
    Color(0xFF4F86C6) to Color(0xFF8FB4E0),
)

/** 和紙風の地の色。 */
private val PAPER = Color(0xFFEBE6DC)

// 一覧の小さな表紙（幅 64dp）の文字。表紙の幅に作品名が 4〜5 文字収まる大きさにしている
private val SMALL_COVER_TITLE = TextStyle(fontSize = 9.5.sp, lineHeight = 13.sp)
private val SMALL_COVER_AUTHOR = TextStyle(fontSize = 8.5.sp, lineHeight = 11.sp)

/** 地に散らす繊維の色。作品 ID を種にした乱数で置くので、同じ作品はいつも同じ模様になる。 */
private val PAPER_FLECK = Color(0x40A08C70)

/** 和紙風の表紙。上に作品名、下の帯に著者名を置き、細い線を 2 本ずつ添える。 */
@Composable
fun BookCover(work: WorkItem, modifier: Modifier = Modifier, small: Boolean = false) {
    val (ink, band) = COVER_COLORS[work.id.mod(COVER_COLORS.size)]
    Column(
        modifier.aspectRatio(0.72f).shadow(1.dp, RoundedCornerShape(3.dp)).background(PAPER).drawBehind {
            val random = Random(work.id)
            repeat(40) {
                val x = random.nextFloat() * size.width
                val y = random.nextFloat() * size.height * 0.68f
                if (it % 4 == 0) {
                    drawLine(
                        PAPER_FLECK,
                        Offset(x, y),
                        Offset(
                            x + 6.dp.toPx() * random.nextFloat(),
                            y + 3.dp.toPx() * (random.nextFloat() - 0.5f),
                        ),
                        0.6.dp.toPx(),
                    )
                } else {
                    drawCircle(PAPER_FLECK, 0.6.dp.toPx(), Offset(x, y))
                }
            }
            val stroke = 1.5.dp.toPx()
            drawLine(band, Offset(0f, 4.dp.toPx()), Offset(size.width * 0.36f, 4.dp.toPx()), stroke)
            drawLine(band, Offset(0f, 9.dp.toPx()), Offset(size.width * 0.2f, 9.dp.toPx()), stroke)
        },
    ) {
        Box(
            Modifier.weight(0.68f).fillMaxWidth()
                .padding(horizontal = if (small) 4.dp else 8.dp)
                .padding(top = if (small) 14.dp else 24.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Text(
                work.title,
                color = ink,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                style = if (small) SMALL_COVER_TITLE else MaterialTheme.typography.titleSmall,
                maxLines = if (small) 2 else 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            Modifier.weight(0.32f).fillMaxWidth().background(band).drawBehind {
                val stroke = 1.5.dp.toPx()
                val line = ink.copy(alpha = 0.5f)
                drawLine(
                    line,
                    Offset(size.width * 0.62f, size.height - 9.dp.toPx()),
                    Offset(
                        size.width,
                        size.height - 9.dp.toPx(),
                    ),
                    stroke,
                )
                drawLine(
                    line,
                    Offset(size.width * 0.8f, size.height - 4.dp.toPx()),
                    Offset(
                        size.width,
                        size.height - 4.dp.toPx(),
                    ),
                    stroke,
                )
            },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                work.authors.orEmpty(),
                color = Color.White.copy(alpha = 0.9f),
                style = if (small) SMALL_COVER_AUTHOR else MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 6.dp),
            )
        }
    }
}
