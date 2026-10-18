package net.clagon.aozora.ui.reader

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.edit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import net.clagon.aozora.AozoraApp
import net.clagon.aozora.catalog.Catalog
import net.clagon.aozora.reader.ReaderSettings
import net.clagon.aozora.reader.readerHtml
import kotlin.math.roundToInt
import android.graphics.Color as AndroidColor

// ページをめくるときに、前のページが画面の外へ抜けるまでの時間
private const val SLIDE_MILLIS = 250

// この距離より長く横に動かしたらスワイプとみなす
private const val SWIPE_DP = 48

/**
 * 本文を縦書きで 1 ページずつ読む画面。本文は WebView に段組みで描き（assets/reader）、
 * めくる操作はその上に重ねた透明な層で受ける。
 * - 左側のタップか右へのスワイプ（紙の本のようにページを右へ送る）で次のページ、右側のタップか左へのスワイプで前のページ。
 * - 中央のタップで、戻るボタンとページのスライダー、文字パネルを開くボタンを出し入れする。
 * 読んだ位置は読了率（0〜1）で作品ごとに残し、次に開いたときにその位置から始める。
 */
// ponytail: 目次・夜間・背景と、音量キーでのめくりはまだない
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ReaderScreen(workId: Int, catalog: Catalog, back: () -> Unit) {
    val context = LocalContext.current
    val texts = (context.applicationContext as AozoraApp).texts
    val work by remember(workId) { catalog.dao.work(workId) }.collectAsState(null)
    val text by produceState<Result<String>?>(null, work?.id) {
        val w = work ?: return@produceState
        value = try {
            Result.success(texts.load(w))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    val positions = remember { context.getSharedPreferences(POSITIONS, Context.MODE_PRIVATE) }
    var progress by rememberSaveable { mutableFloatStateOf(positions.getFloat(workId.toString(), 0f)) }
    // 段組みが終わるまでは 0
    var pages by remember { mutableIntStateOf(0) }
    val page = if (pages <= 1) 0 else (progress * (pages - 1)).roundToInt()
    var barsShown by remember { mutableStateOf(false) }
    var panelShown by remember { mutableStateOf(false) }
    var readerSettings by remember { mutableStateOf(ReaderSettings.load(context)) }

    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            setBackgroundColor(AndroidColor.WHITE)
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            addJavascriptInterface(
                object {
                    // JavaScript のスレッドから呼ばれるので、画面のスレッドに移してから状態を変える
                    @JavascriptInterface
                    fun onLayout(count: Int) = post { pages = count }
                },
                "Android",
            )
        }
    }
    DisposableEffect(webView) { onDispose { webView.destroy() } }
    LaunchedEffect(text) {
        val html = text?.getOrNull() ?: return@LaunchedEffect
        val assets = context.assets
        val css = assets.open("reader/reader.css").bufferedReader().use { it.readText() }
        val script = assets.open("reader/reader.js").bufferedReader().use { it.readText() }
        // 挿絵や外字の画像を相対パスで読めるよう、本文の URL を基準にする
        val page = readerHtml(html, css, readerSettings.cssVariables(), script)
        webView.loadDataWithBaseURL(work?.xhtmlUrl, page, "text/html", "utf-8", null)
    }
    // ページ数か読了率が変わったら、そのページを見せ、読んだ位置を残す
    LaunchedEffect(page, pages) {
        if (pages == 0) return@LaunchedEffect
        webView.evaluateJavascript("reader.go($page)", null)
        positions.edit { putFloat(workId.toString(), progress) }
    }

    // めくる前のページの画像。めくったあと、これを画面の外へ滑らせる
    var leaving by remember { mutableStateOf<ImageBitmap?>(null) }
    val slide = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val shown = rememberGraphicsLayer()
    fun jump(to: Int) {
        progress = if (pages <= 1) 0f else to.coerceIn(0, pages - 1).toFloat() / (pages - 1)
    }
    fun turn(delta: Int) {
        val from = if (pages <= 1) 0 else (progress * (pages - 1)).roundToInt()
        val to = from + delta
        if (to !in 0 until pages) return
        if (!readerSettings.slide) return jump(to)
        scope.launch {
            // WebView 自身の drawToBitmap は画面と違う大きさで描くことがあるので、画面に描いたものを写す
            leaving = shown.toImageBitmap()
            jump(to)
            slide.snapTo(0f)
            // 縦書きは右から左へ進むので、次へめくるときは前のページが右へ抜ける
            slide.animateTo(if (delta > 0) 1f else -1f, tween(SLIDE_MILLIS))
            leaving = null
        }
    }

    Box(Modifier.fillMaxSize().background(Color.White).systemBarsPadding()) {
        when {
            text == null -> {}

            text!!.isFailure -> Text(
                text!!.exceptionOrNull()?.message ?: "本文を読み込めませんでした",
                Modifier.align(Alignment.Center).padding(24.dp),
            )

            else -> AndroidView(
                { webView },
                Modifier.fillMaxSize().drawWithContent {
                    shown.record { this@drawWithContent.drawContent() }
                    drawLayer(shown)
                },
            )
        }
        leaving?.let {
            Image(
                it,
                contentDescription = null,
                // 写したページは背景が透けるので、下の新しいページが見えないよう白で塗る
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    translationX = slide.value * size.width
                }.background(Color.White),
            )
        }
        Box(
            Modifier.fillMaxSize()
                .pointerInput(Unit) {
                    val width = size.width
                    detectTapGestures { offset ->
                        when {
                            offset.x < width / 3 -> turn(1)
                            offset.x > width * 2 / 3 -> turn(-1)
                            else -> barsShown = !barsShown
                        }
                    }
                }
                .pointerInput(Unit) {
                    val swipe = SWIPE_DP.dp.toPx()
                    var dx = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { dx = 0f },
                        onDragEnd = {
                            if (dx > swipe) turn(1)
                            if (dx < -swipe) turn(-1)
                        },
                    ) { _, amount -> dx += amount }
                },
        )
        if (pages > 0) {
            Text(
                "${page + 1}/$pages　${"%.2f".format(if (pages <= 1) 100f else page * 100f / (pages - 1))}%",
                color = MaterialTheme.colorScheme.outline,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 8.dp),
            )
        }
        if (barsShown) {
            IconButton(
                onClick = back,
                modifier = Modifier.padding(12.dp).size(44.dp).clip(CircleShape).background(Color(0x99000000)),
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBackIos, contentDescription = "戻る", tint = Color.White)
            }
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .navigationBarsPadding().padding(horizontal = 24.dp, vertical = 12.dp),
            ) {
                // 1 ページしかない作品では動かす先がないので、スライダーだけ出さない
                if (pages > 1) {
                    Slider(
                        value = page.toFloat(),
                        onValueChange = { jump(it.roundToInt()) },
                        valueRange = 0f..(pages - 1).toFloat(),
                        colors = SliderDefaults.colors(
                            inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        ),
                    )
                }
                // ponytail: 目次・夜間・背景は、作るときにこの並びの左に足す
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    BarButton(Icons.Filled.TextFields, "文字") { panelShown = true }
                }
            }
        }
    }
    if (panelShown) {
        TextPanel(
            readerSettings,
            onChange = {
                readerSettings = it
                it.save(context)
                // ページ数が変わっても読了率は変わらないので、組み直したあとは同じあたりのページを見せる
                webView.evaluateJavascript("reader.style('${it.cssVariables()}')", null)
            },
            onDismiss = { panelShown = false },
        )
    }
}

/** 下部のバーに並べる、アイコンと名前のボタン。 */
@Composable
private fun BarButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        Modifier.clip(
            RoundedCornerShape(8.dp),
        ).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
    }
}

/** 読んだ位置（作品 ID → 読了率）を残す SharedPreferences の名前。 */
// ponytail: 本棚・読書履歴の DB を作るときに、そちらへ移す
private const val POSITIONS = "reading_positions"
