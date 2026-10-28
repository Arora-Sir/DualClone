package com.mohitarora.dualclone.core.designsystem

import android.os.Build
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

/** True when the user turned system animations off (animator duration scale 0). */
@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }
}

/** Springs back instantly on release. Skipped when animations are off. */
fun Modifier.dcPressScale(
    interactionSource: MutableInteractionSource,
    scale: Float = DcMotion.pressScale
): Modifier = composed {
    val reduce = rememberReduceMotion()
    val pressed by interactionSource.collectIsPressedAsState()
    val factor by animateFloatAsState(
        targetValue = if (pressed && !reduce) scale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "pressScale"
    )
    graphicsLayer {
        scaleX = factor
        scaleY = factor
    }
}

/** Haptic feedback roles from Blueprint Pillar 10.7. Falls back to the closest constant below the API that added each one. */
@Immutable
class DcHaptics(private val view: View) {
    fun longPress() = perform(HapticFeedbackConstants.LONG_PRESS)

    fun success() = perform(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.CONTEXT_CLICK
    )

    fun error() = perform(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS
    )

    fun toggle(on: Boolean) = perform(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (on) HapticFeedbackConstants.TOGGLE_ON else HapticFeedbackConstants.TOGGLE_OFF
        } else {
            HapticFeedbackConstants.CLOCK_TICK
        }
    )

    private fun perform(constant: Int) {
        view.performHapticFeedback(constant)
    }
}

@Composable
fun rememberDcHaptics(): DcHaptics {
    val view = LocalView.current
    return remember(view) { DcHaptics(view) }
}

/** Keeps the system back gesture off this element, for edge rails such as the alphabet index. Android 10 and later. */
fun Modifier.dcSystemGestureExclusion(): Modifier = composed {
    val view = LocalView.current
    var rect by remember { mutableStateOf<android.graphics.Rect?>(null) }
    DisposableEffect(view, rect) {
        val r = rect
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && r != null) view.systemGestureExclusionRects = listOf(r)
        onDispose { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) view.systemGestureExclusionRects = emptyList() }
    }
    onGloballyPositioned { coords ->
        val b = coords.boundsInWindow()
        rect = android.graphics.Rect(b.left.toInt(), b.top.toInt(), b.right.toInt(), b.bottom.toInt())
    }
}
