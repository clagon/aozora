package net.clagon.aozora.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScreenTest {
    @Test
    fun screensSurviveSaveAndRestore() {
        // アプリが止められたときは積んだ画面を文字列にして保存し、戻ってきたときに読み直す
        val stack = listOf(
            Screen.NewWorks,
            Screen.Ndc("91", kidsOnly = true),
            Screen.NdcWorks("913", kidsOnly = false),
            Screen.Ranking,
            Screen.Search,
            Screen.KanaWorks("あ"),
            Screen.PersonDetail(879, "芥川 竜之介"),
            Screen.WorkDetail(127),
            Screen.Reader(127),
            Screen.FavoritePersons,
        )
        assertEquals(stack, stack.map { restoreScreen(it.saveKey()) })
        // 前の版などで保存した知らない形は戻さない
        assertNull(restoreScreen("Bookmark\t1"))
        assertNull(restoreScreen("Reader\tx"))
        assertNull(restoreScreen("Ndc\t91"))
    }
}
