package com.mohitarora.dualclone.core.designsystem.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import com.mohitarora.dualclone.core.designsystem.DcSizes
import com.mohitarora.dualclone.core.designsystem.rememberReduceMotion

// NOTE: A fixed red keeps the heart red in every theme, because the dark error role is a soft pink that reads as a pastel heart.
private val HeartRed = Color(0xFFE53935)

/**
 * The red heart of the Support row. It is a bare glyph in the same 24dp slot as every other row icon, so titles line up.
 * The soft double beat scales through a graphics layer, which never changes layout, and it stops when system animations are off.
 */
@Composable
fun DcHeartbeatIcon(modifier: Modifier = Modifier) {
    val scale = if (rememberReduceMotion()) {
        1f
    } else {
        val transition = rememberInfiniteTransition(label = "heartbeat")
        val beat by transition.animateFloat(
            initialValue = 1f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 1600
                    1f at 0
                    1.18f at 140
                    1f at 280
                    1.10f at 420
                    1f at 560
                    1f at 1600
                },
                repeatMode = RepeatMode.Restart
            ),
            label = "heartbeatScale"
        )
        beat
    }
    Icon(
        imageVector = Icons.Default.Favorite,
        contentDescription = null,
        tint = HeartRed,
        modifier = modifier
            .size(DcSizes.settingsIconGlyph)
            .graphicsLayer { scaleX = scale; scaleY = scale }
    )
}
