package net.clagon.aozora.reader

import android.os.SystemClock

/** 1 ページにこれより長くいた分は、読まずに置いていたとみなして数えない。 */
const val MAX_PAGE_MILLIS = 5 * 60_000L

/**
 * 読書時間を数える。start から stop まで（リーダーが前に出ている間）を、ページをめくるたび（lap）に record へ渡す。
 * 画面を消灯しない設定で置きっぱなしにしても増え続けないよう、1 ページあたり [MAX_PAGE_MILLIS] までにする。
 */
// ponytail: 日付をまたいで読んでいると、その区切りの時間はすべて書き込んだ日に入る
class ReadingClock(private val record: (Long) -> Unit, private val now: () -> Long = SystemClock::elapsedRealtime) {
    private var since = -1L

    fun start() {
        since = now()
    }

    fun lap() {
        if (since < 0) return
        val t = now()
        record(minOf(t - since, MAX_PAGE_MILLIS))
        since = t
    }

    fun stop() {
        lap()
        since = -1
    }
}
