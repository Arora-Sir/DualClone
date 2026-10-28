package com.mohitarora.dualclone.core.designsystem

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * HSV and hex conversions for the custom color picker, in pure Kotlin so JVM tests can prove them.
 * Channels are rounded, never truncated, so a hex code survives hex to HSV to hex unchanged.
 */
object DcColorMath {

    /** Opaque ARGB from hue in degrees and saturation and value in 0..1. */
    fun hsvToArgb(hue: Float, saturation: Float, value: Float): Int {
        val h = (((hue % 360f) + 360f) % 360f) / 60f
        val s = saturation.coerceIn(0f, 1f)
        val v = value.coerceIn(0f, 1f)
        val c = v * s
        val x = c * (1f - abs(h % 2f - 1f))
        val m = v - c
        val (r, g, b) = when (h.toInt()) {
            0 -> Triple(c, x, 0f)
            1 -> Triple(x, c, 0f)
            2 -> Triple(0f, c, x)
            3 -> Triple(0f, x, c)
            4 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        return (0xFF shl 24) or (channel(r + m) shl 16) or (channel(g + m) shl 8) or channel(b + m)
    }

    /** Hue in degrees, saturation and value in 0..1. */
    fun argbToHsv(argb: Int): FloatArray {
        val r = ((argb shr 16) and 0xFF) / 255f
        val g = ((argb shr 8) and 0xFF) / 255f
        val b = (argb and 0xFF) / 255f
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min
        var hue = when {
            delta == 0f -> 0f
            max == r -> 60f * (((g - b) / delta) % 6f)
            max == g -> 60f * ((b - r) / delta + 2f)
            else -> 60f * ((r - g) / delta + 4f)
        }
        if (hue < 0f) hue += 360f
        return floatArrayOf(hue, if (max == 0f) 0f else delta / max, max)
    }

    fun toHex(argb: Int): String = "%06X".format(argb and 0xFFFFFF)

    /** Six hex digits, with or without a leading hash, to an opaque ARGB. Null when it is not exactly six hex digits. */
    fun parseHex(text: String): Int? {
        val clean = text.removePrefix("#")
        if (clean.length != 6 || clean.any { it !in "0123456789abcdefABCDEF" }) return null
        return (0xFF shl 24) or clean.toInt(16)
    }

    private fun channel(f: Float): Int = (f * 255f).roundToInt().coerceIn(0, 255)
}
