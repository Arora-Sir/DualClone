package com.mohitarora.dualclone.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min

/** WCAG contrast helpers. Pure Kotlin so they run in JVM unit tests. */
object DcContrast {
    const val BADGE_CONTAINER_MIN = 3.0f
    const val TEXT_MIN = 4.5f

    fun ratio(a: Color, b: Color): Float {
        val la = a.luminance() + 0.05f
        val lb = b.luminance() + 0.05f
        return max(la, lb) / min(la, lb)
    }

    /** Alpha-blends [fg] over an opaque [bg]. */
    fun over(fg: Color, bg: Color): Color = fg.compositeOver(bg)

    /** Moves [fg] toward black or white until it reaches [minRatio] against [bg]. Returns [fg] unchanged when it already passes. */
    fun ensure(fg: Color, bg: Color, minRatio: Float): Color {
        if (ratio(fg, bg) >= minRatio) return fg
        val target = if (bg.luminance() > 0.4f) Color.Black else Color.White
        var step = 1
        while (step <= 50) {
            val candidate = lerp(fg, target, step / 50f)
            if (ratio(candidate, bg) >= minRatio) return candidate
            step++
        }
        return target
    }

    /** Black or white, whichever reads better on [bg]. */
    fun onColor(bg: Color): Color =
        if (ratio(Color.White, bg) >= ratio(Color.Black, bg)) Color.White else Color.Black
}
