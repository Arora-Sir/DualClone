package com.mohitarora.dualclone.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.mohitarora.dualclone.R
import com.mohitarora.dualclone.core.designsystem.DcSpacing
import kotlin.math.roundToInt

private const val KEYBOARD_STEP = 0.05f
private const val HUE_STEP = 10f

/**
 * A saturation and brightness square with a draggable pointer, and a hue strip below it.
 * The values are plain HSV, owned by the caller. Every drag reports through [onChange], and each part also exposes
 * screen reader actions, because a drag surface alone cannot be used with TalkBack.
 */
@Composable
fun DcColorPicker(
    hue: Float,
    saturation: Float,
    value: Float,
    onChange: (hue: Float, saturation: Float, value: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentHue by rememberUpdatedState(hue)
    val currentSaturation by rememberUpdatedState(saturation)
    val currentValue by rememberUpdatedState(value)
    val currentOnChange by rememberUpdatedState(onChange)

    val squareLabel = stringResource(R.string.custom_color_square)
    val squareState = stringResource(R.string.custom_color_square_state, (saturation * 100).roundToInt(), (value * 100).roundToInt())
    val moreSaturated = stringResource(R.string.custom_color_more_saturated)
    val lessSaturated = stringResource(R.string.custom_color_less_saturated)
    val brighter = stringResource(R.string.custom_color_brighter)
    val darker = stringResource(R.string.custom_color_darker)
    val hueLabel = stringResource(R.string.custom_color_hue)
    val hueState = stringResource(R.string.custom_color_hue_state, hue.roundToInt())

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(DcSpacing.lg)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        fun report(p: Offset) {
                            val s = (p.x / size.width).coerceIn(0f, 1f)
                            val v = 1f - (p.y / size.height).coerceIn(0f, 1f)
                            currentOnChange(currentHue, s, v)
                        }
                        val down = awaitFirstDown()
                        report(down.position)
                        down.consume()
                        drag(down.id) { change ->
                            report(change.position)
                            change.consume()
                        }
                    }
                }
                .semantics {
                    contentDescription = squareLabel
                    stateDescription = squareState
                    customActions = listOf(
                        CustomAccessibilityAction(moreSaturated) {
                            currentOnChange(currentHue, (currentSaturation + KEYBOARD_STEP).coerceAtMost(1f), currentValue); true
                        },
                        CustomAccessibilityAction(lessSaturated) {
                            currentOnChange(currentHue, (currentSaturation - KEYBOARD_STEP).coerceAtLeast(0f), currentValue); true
                        },
                        CustomAccessibilityAction(brighter) {
                            currentOnChange(currentHue, currentSaturation, (currentValue + KEYBOARD_STEP).coerceAtMost(1f)); true
                        },
                        CustomAccessibilityAction(darker) {
                            currentOnChange(currentHue, currentSaturation, (currentValue - KEYBOARD_STEP).coerceAtLeast(0f)); true
                        }
                    )
                }
        ) {
            val corner = CornerRadius(16.dp.toPx())
            drawRoundRect(Brush.horizontalGradient(listOf(Color.White, Color.hsv(hue, 1f, 1f))), cornerRadius = corner)
            drawRoundRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)), cornerRadius = corner)
            drawPointer(Offset(saturation * size.width, (1f - value) * size.height), Color.hsv(hue, saturation, value))
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        fun report(p: Offset) {
                            currentOnChange((p.x / size.width).coerceIn(0f, 1f) * 360f, currentSaturation, currentValue)
                        }
                        val down = awaitFirstDown()
                        report(down.position)
                        down.consume()
                        drag(down.id) { change ->
                            report(change.position)
                            change.consume()
                        }
                    }
                }
                .semantics {
                    contentDescription = hueLabel
                    stateDescription = hueState
                    progressBarRangeInfo = ProgressBarRangeInfo(currentHue, 0f..360f)
                    setProgress { target ->
                        currentOnChange(target.coerceIn(0f, 360f), currentSaturation, currentValue); true
                    }
                    customActions = listOf(
                        CustomAccessibilityAction(hueLabel + " +") { currentOnChange((currentHue + HUE_STEP) % 360f, currentSaturation, currentValue); true },
                        CustomAccessibilityAction(hueLabel + " -") { currentOnChange((currentHue - HUE_STEP + 360f) % 360f, currentSaturation, currentValue); true }
                    )
                }
        ) {
            drawRoundRect(
                Brush.horizontalGradient(List(7) { Color.hsv(it * 60f, 1f, 1f) }),
                cornerRadius = CornerRadius(size.height / 2f)
            )
            drawPointer(Offset((hue / 360f) * size.width, size.height / 2f), Color.hsv(hue, 1f, 1f))
        }
    }
}

/** A ring with a dark hairline, so it stays visible on light and dark parts of the gradient. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPointer(center: Offset, fill: Color) {
    drawCircle(Color.Black.copy(alpha = 0.35f), radius = 15.5.dp.toPx(), center = center, style = Stroke(1.dp.toPx()))
    drawCircle(Color.White, radius = 14.dp.toPx(), center = center, style = Stroke(3.dp.toPx()))
    drawCircle(fill, radius = 11.dp.toPx(), center = center)
}
