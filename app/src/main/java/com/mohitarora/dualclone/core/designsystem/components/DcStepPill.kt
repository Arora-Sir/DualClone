package com.mohitarora.dualclone.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * A pill track whose rounded fill marks the level, with a tick circle for each step from 0 to [steps].
 * Tap anywhere on the track or drag along it. [description] is the spoken name and [valueText] the spoken value.
 */
@Composable
fun DcStepPill(
    value: Int,
    steps: Int,
    onValueChange: (Int) -> Unit,
    description: String,
    valueText: String,
    modifier: Modifier = Modifier
) {
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val fill = lerp(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary, 0.45f)
    val tickOff = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    val tickOn = MaterialTheme.colorScheme.onPrimaryContainer
    val haptics = LocalHapticFeedback.current
    val current = rememberUpdatedState(value)
    val emit = rememberUpdatedState(onValueChange)

    fun pick(x: Float, width: Float, pad: Float): Int {
        val span = (width - 2 * pad).coerceAtLeast(1f)
        return (((x - pad) / span).coerceIn(0f, 1f) * steps).roundToInt()
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .semantics {
                contentDescription = description
                stateDescription = valueText
                progressBarRangeInfo = ProgressBarRangeInfo(value.toFloat(), 0f..steps.toFloat(), steps - 1)
                setProgress { target ->
                    emit.value(target.roundToInt().coerceIn(0, steps))
                    true
                }
            }
            .pointerInput(steps) {
                val pad = 20.dp.toPx()
                detectTapGestures { offset ->
                    val next = pick(offset.x, size.width.toFloat(), pad)
                    if (next != current.value) { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); emit.value(next) }
                }
            }
            .pointerInput(steps) {
                val pad = 20.dp.toPx()
                detectHorizontalDragGestures { change, _ ->
                    val next = pick(change.position.x, size.width.toFloat(), pad)
                    if (next != current.value) { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); emit.value(next) }
                }
            }
    ) {
        val pad = 20.dp.toPx()
        val span = size.width - 2 * pad
        val edge = pad + span * value / steps
        val radius = CornerRadius(size.height / 2f)
        drawRoundRect(track, size = size, cornerRadius = radius)
        // NOTE: The rounded end of the fill is the indicator, so it extends past the last filled tick by a pad to look like a pill.
        drawRoundRect(fill, size = Size((edge + 14.dp.toPx()).coerceAtMost(size.width), size.height), cornerRadius = radius)
        for (i in 0..steps) {
            drawCircle(if (i <= value) tickOn else tickOff, radius = 3.dp.toPx(), center = Offset(pad + span * i / steps, size.height / 2f))
        }
    }
}
