package com.mohitarora.dualclone.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

// NOTE: Values come from the Mohit Arora Android Trademark Blueprint, Pillar 10.5.
object DcSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val minTouchTarget = 48.dp
}

object DcShapes {
    val chip = RoundedCornerShape(8.dp)
    val field = RoundedCornerShape(12.dp)
    val card = RoundedCornerShape(16.dp)
    /** Grouped settings sections, Blueprint 10.5. Rows inside are separated by a 2dp gap. */
    val group = RoundedCornerShape(24.dp)
    val hero = RoundedCornerShape(20.dp)
    val button = RoundedCornerShape(24.dp)
    val sheet = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    val dialog = RoundedCornerShape(28.dp)
}

object DcMotion {
    const val pressScale = 0.97f
    const val fastMillis = 150
    const val standardMillis = 220
    const val themeCrossfadeMillis = 400
    val slideDistance = 96.dp
}

object DcSizes {
    val settingsMaxWidth = 640.dp
    val sheetMaxWidth = 640.dp
    val appIcon = 44.dp
    val settingsIconCircle = 40.dp
    val settingsIconGlyph = 24.dp
    val buttonIconSlot = 18.dp
    val minButtonHeight = 48.dp
    val paletteCell = 72.dp
    val paletteCircle = 40.dp
    val expandedBreakpoint = 840.dp
    val compactBreakpoint = 360.dp
    /** A button row stacks only below this width. Two buttons need about 150dp each, so a 336dp sheet fits them and a 270dp card does not. */
    val buttonRowStackWidth = 320.dp
}
