package net.clagon.aozora.reader

import android.os.SystemClock

/** 1 ページにこれより長くいた分は、読まずに置いていたとみなして数えない。 */
const val MAX_PAGE_MILLIS = 5 * 60_000L

/**
 * 読書時間を数える。start から stop まで（リーダーが前に出ている間）を、ページをめくるたび（lap）に record へ渡す。
 * 画面を消灯しない設定で置きっぱなしにしても増え続けないよう、1 ページあたり [MAX_PAGE_MILLIS] までにする。
 * record には、数えた時間と、それが何ミリ秒前に終わったか（上限で切った分。切らなければ 0）を渡す。
 */
class ReadingClock(
    private val record: (millis: Long, endedAgo: Long) -> Unit,
    private val now: () -> Long = SystemClock::elapsedRealtime,
) {
    private var since = -1L

    fun start() {
        since = now()
    }

    fun lap() {
        if (since < 0) return
        val t = now()
        val millis = minOf(t - since, MAX_PAGE_MILLIS)
        record(millis, t - since - millis)
        since = t
    }

    fun stop() {
        lap()
        since = -1
    }
}
