package com.mohitarora.dualclone

import com.mohitarora.dualclone.core.designsystem.DcColorMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.random.Random

class DcColorMathTest {

    private fun roundTrip(argb: Int): Int {
        val hsv = DcColorMath.argbToHsv(argb)
        return DcColorMath.hsvToArgb(hsv[0], hsv[1], hsv[2])
    }

    @Test
    fun primariesAndExtremesAreExact() {
        assertEquals(0xFF000000.toInt(), DcColorMath.hsvToArgb(0f, 0f, 0f))
        assertEquals(0xFFFFFFFF.toInt(), DcColorMath.hsvToArgb(0f, 0f, 1f))
        assertEquals(0xFFFF0000.toInt(), DcColorMath.hsvToArgb(0f, 1f, 1f))
        assertEquals(0xFF00FF00.toInt(), DcColorMath.hsvToArgb(120f, 1f, 1f))
        assertEquals(0xFF0000FF.toInt(), DcColorMath.hsvToArgb(240f, 1f, 1f))
        assertEquals(0xFFFF0000.toInt(), DcColorMath.hsvToArgb(360f, 1f, 1f))
    }

    /** The case that used to drift: truncating a channel turned 00BCD4 into 00BBD4 after one trip. */
    @Test
    fun defaultCustomColorSurvivesTheTrip() {
        val argb = DcColorMath.parseHex("00BCD4")!!
        assertEquals("00BCD4", DcColorMath.toHex(roundTrip(argb)))
    }

    @Test
    fun everyColorOnAGridSurvivesTheTrip() {
        val steps = (0..255 step 15).toList() + 255
        for (r in steps) for (g in steps) for (b in steps) {
            val argb = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            assertEquals("%06X".format(argb and 0xFFFFFF), DcColorMath.toHex(roundTrip(argb)))
        }
    }

    @Test
    fun randomColorsSurviveTheTrip() {
        val random = Random(42)
        repeat(20_000) {
            val argb = (0xFF shl 24) or random.nextInt(0x1000000)
            assertEquals("%06X".format(argb and 0xFFFFFF), DcColorMath.toHex(roundTrip(argb)))
        }
    }

    @Test
    fun hexParsingAcceptsOnlySixDigits() {
        assertEquals(0xFF112233.toInt(), DcColorMath.parseHex("112233"))
        assertEquals(0xFF112233.toInt(), DcColorMath.parseHex("#112233"))
        assertEquals(0xFFAABBCC.toInt(), DcColorMath.parseHex("aabbcc"))
        assertNull(DcColorMath.parseHex("11223"))
        assertNull(DcColorMath.parseHex("1122334"))
        assertNull(DcColorMath.parseHex("11223G"))
        assertNull(DcColorMath.parseHex(""))
    }

    @Test
    fun hueWrapsAround() {
        assertEquals(DcColorMath.hsvToArgb(10f, 1f, 1f), DcColorMath.hsvToArgb(370f, 1f, 1f))
        assertEquals(DcColorMath.hsvToArgb(350f, 1f, 1f), DcColorMath.hsvToArgb(-10f, 1f, 1f))
    }
}
