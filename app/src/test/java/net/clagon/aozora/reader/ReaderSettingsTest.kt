package net.clagon.aozora.reader

import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderSettingsTest {
    @Test
    fun nightOverridesTheBackgroundAndInvertsGaiji() {
        val cream = ReaderSettings(background = Background.Cream).cssVariables()
        assertTrue(cream, "--paper:#f6f0e1;--ink:#3b3328;--gaiji-filter:none" in cream)
        val night = ReaderSettings(background = Background.Cream, night = true).cssVariables()
        assertTrue(night, "--paper:#1b1b1d;--ink:#a8a8a8;--gaiji-filter:invert(1)" in night)
    }
}
