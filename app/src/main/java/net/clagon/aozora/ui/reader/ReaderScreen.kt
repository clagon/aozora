package net.clagon.aozora.ui.reader

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import net.clagon.aozora.AozoraApp
import net.clagon.aozora.catalog.Catalog
import net.clagon.aozora.reader.Heading
import net.clagon.aozora.reader.ReaderSettings
import net.clagon.aozora.reader.current
import net.clagon.aozora.reader.nextChapter
import net.clagon.aozora.reader.parseHeadings
import net.clagon.aozora.reader.previousChapter
import net.clagon.aozora.reader.readerHtml
import java.net.URLConnection
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
 * - 中央のタップで、戻るボタン、ページのスライダーと前後の章への移動、目次と文字パネルを開くボタンを出し入れする。
 * 読んだ位置は読了率（0〜1）で作品ごとに残し（library.db）、次に開いたときにその位置から始める。開くと読書履歴に入る。
 */
// ponytail: 夜間・背景と、音量キーでのめくりはまだない
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ReaderScreen(workId: Int, catalog: Catalog, back: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as AozoraApp
    val library = app.library.dao
    val work by remember(workId) { catalog.dao.work(workId) }.collectAsState(null)
    // 読了率。残した位置を読むまでは -1（読む前に組み終わっても 0 ページ目を残さないように）
    var progress by rememberSaveable { mutableFloatStateOf(-1f) }
    val text by produceState<Result<String>?>(null, work?.id) {
        val w = work ?: return@produceState
        if (progress < 0) progress = library.progress(workId) ?: 0f
        library.open(workId)
        value = try {
            Result.success(app.texts.load(w))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 段組みが終わるまでは 0
    var pages by remember { mutableIntStateOf(0) }
    val page = if (pages <= 1) 0 else (progress * (pages - 1)).roundToInt()
    var barsShown by remember { mutableStateOf(false) }
    var panelShown by remember { mutableStateOf(false) }
    var tocShown by remember { mutableStateOf(false) }
    // 見出しとそのページ。段組みをやり直すたびに数え直す
    var headings by remember { mutableStateOf(emptyList<Heading>()) }
    var readerSettings by remember { mutableStateOf(ReaderSettings.load(context)) }

    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            setBackgroundColor(AndroidColor.WHITE)
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            // 挿絵と外字の画像は端末に残したものを使い、なければ取ってきて残す（オフラインでも読めるように）
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                    val url = request.url.toString()
                    val image = app.images.open(url) ?: return null
                    return WebResourceResponse(URLConnection.guessContentTypeFromName(url), null, image)
                }
            }
            addJavascriptInterface(
                object {
                    // JavaScript のスレッドから呼ばれるので、画面のスレッドに移してから状態を変える
                    @JavascriptInterface
                    fun onLayout(count: Int, found: String) = post {
                        pages = count
                        headings = parseHeadings(found)
                    }
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
        library.saveProgress(workId, progress)
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
                Icon(
                    Icons.Filled.ArrowBackIosNew,
                    contentDescription = "戻る",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .navigationBarsPadding().padding(horizontal = 24.dp, vertical = 12.dp),
            ) {
                // 1 ページしかない作品では動かす先がないので、スライダーと章の移動は出さない
                if (pages > 1) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val previous = headings.previousChapter(page)
                        val next = headings.nextChapter(page)
                        TextButton(onClick = { previous?.let(::jump) }, enabled = previous != null) { Text("前の章") }
                        Slider(
                            value = page.toFloat(),
                            onValueChange = { jump(it.roundToInt()) },
                            valueRange = 0f..(pages - 1).toFloat(),
                            colors = SliderDefaults.colors(
                                inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            ),
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { next?.let(::jump) }, enabled = next != null) { Text("次の章") }
                    }
                }
                // ponytail: 夜間・背景は、作るときに目次と文字のあいだに足す
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    BarButton(Icons.AutoMirrored.Filled.List, "目次") { tocShown = true }
                    BarButton(Icons.Filled.TextFields, "文字") { panelShown = true }
                }
            }
        }
    }
    if (tocShown) {
        TocPanel(
            headings,
            headings.current(page),
            onSelect = {
                jump(it.page)
                tocShown = false
            },
            onDismiss = { tocShown = false },
        )
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
