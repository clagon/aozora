package net.clagon.aozora.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextsTest {
    @Test
    fun decodeXhtmlFollowsTheXmlDeclaration() {
        val body = "吾輩は猫である～"
        val sjis = """<?xml version="1.0" encoding="Shift_JIS"?>$body""".toByteArray(charset("windows-31j"))
        assertTrue(decodeXhtml(sjis).endsWith(body))
        val utf8 = """<?xml version="1.0" encoding="UTF-8"?>$body""".toByteArray()
        assertTrue(decodeXhtml(utf8).endsWith(body))
    }

    @Test
    fun readerHtmlSwapsTheSiteStylesForTheReaders() {
        val xhtml = """<head>
	<link rel="stylesheet" type="text/css" href="../../aozora.css" />
	<script type="text/javascript" src="../../jquery-1.4.2.min.js"></script>
</head><body>本文</body>"""
        val html = readerHtml(xhtml, "body{}", "go()")
        assertFalse(html.contains("aozora.css"))
        assertFalse(html.contains("jquery"))
        assertEquals(1, Regex("<style>body\\{\\}</style><script>go\\(\\)</script></head>").findAll(html).count())
    }
}
