package net.clagon.aozora.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

class ImagesTest {
    private val dir = File("build/images-test")
    private val images = Images(dir)

    @Test
    fun keepsTheSitePath() {
        assertEquals(
            File(dir, "cards/000148/files/789_14547_files/fig1.png"),
            images.fileFor("https://www.aozora.gr.jp/cards/000148/files/789_14547_files/fig1.png"),
        )
        // 本文から ../../../gaiji/… で指す外字は、作品をまたいで同じ場所になる
        assertEquals(
            File(dir, "gaiji/1-02/1-02-22.png"),
            images.fileFor("https://www.aozora.gr.jp/cards/000148/files/../../../gaiji/1-02/1-02-22.png"),
        )
    }

    @Test
    fun ignoresOtherSitesAndNonImagesAndPathsOutside() {
        assertNull(images.fileFor("https://example.com/fig1.png"))
        assertNull(images.fileFor("https://www.aozora.gr.jp/cards/000148/files/789_14547.html"))
        assertNull(images.fileFor("https://www.aozora.gr.jp/../../secret.png"))
        assertNull(images.fileFor("file:///data/fig1.png"))
    }
}
