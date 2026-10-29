package com.mohitarora.dualclone.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt

val BrandPurple = Color(0xFF8C52FF)
val BrandOrange = Color(0xFFFF6D00)

/** The surface lift the owner recommends. It reproduces the Super-Artemis Pure black look, with cards on 0D0D0D. */
const val DEFAULT_SURFACE_LIFT = 50

/** One Pure black ladder. Only the surface steps move with the lift, so contrast of every accent role stays tonal. */
internal data class AmoledLadder(val black: Color, val container: Color, val high: Color, val highest: Color, val outlineVariant: Color)

/**
 * Builds the Pure black ladder for a lift from 0 to 100.
 * Each step is interpolated through three anchors: the flat end (0), the recommended look (50) and the lifted end (100).
 * The flat end still keeps a visible gap from the page, because the contrast tests require every step to differ.
 * All greys are neutral with no tint.
 */
internal fun amoledLadder(lift: Int = DEFAULT_SURFACE_LIFT): AmoledLadder {
    val l = lift.coerceIn(0, 100)
    fun step(flat: Int, recommended: Int, lifted: Int): Color {
        val v = if (l <= 50) flat + (recommended - flat) * l / 50f else recommended + (lifted - recommended) * (l - 50) / 50f
        val c = v.roundToInt().coerceIn(0, 255)
        return Color(0xFF000000.toInt() or (c shl 16) or (c shl 8) or c)
    }
    return AmoledLadder(
        black = Color(0xFF000000),
        container = step(4, 13, 27),
        high = step(8, 22, 42),
        highest = step(12, 31, 52),
        outlineVariant = Color(0xFF333333)
    )
}
