package net.clagon.aozora.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class ScreenTest {
    @Test
    fun screensSurviveSerialization() {
        // アプリが止められたときは積んだ画面を Bundle に書き出し、戻ってきたときに読み直す
        val stack = arrayListOf(
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
        val bytes = ByteArrayOutputStream().also { ObjectOutputStream(it).writeObject(stack) }.toByteArray()
        assertEquals(stack, ObjectInputStream(ByteArrayInputStream(bytes)).readObject())
    }
}
