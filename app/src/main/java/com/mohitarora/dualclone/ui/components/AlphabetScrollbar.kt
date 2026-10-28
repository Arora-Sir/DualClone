package com.mohitarora.dualclone.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import com.mohitarora.dualclone.core.designsystem.dcSystemGestureExclusion
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.mohitarora.dualclone.R
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohitarora.dualclone.ui.viewmodel.SortOrder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AlphabetScrollbar(
    onLetterSelected: (Char) -> Unit,
    sortOrder: SortOrder = SortOrder.NAME_ASC,
    modifier: Modifier = Modifier
) {
    val alphabet = remember(sortOrder) {
        if (sortOrder == SortOrder.NAME_DESC) {
            ('Z' downTo 'A').toList() + listOf('#')
        } else {
            listOf('#') + ('A'..'Z').toList()
        }
    }
    var activeLetter by remember { mutableStateOf<Char?>(null) }
    var lastLetter by remember { mutableStateOf<Char?>(null) }
    var isDragging by remember { mutableStateOf(false) }
    var touchY by remember { mutableFloatStateOf(0f) }
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val railDescription = stringResource(R.string.list_alphabet_rail)
    var hideJob by remember { mutableStateOf<Job?>(null) }

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val totalHeightDp = maxHeight
        val totalHeightPx = with(density) { totalHeightDp.toPx() }
        // NOTE: Show every n-th letter when the rail is short, so letters never get clipped.
        val step = remember(totalHeightDp, alphabet) {
            val perLetter = ((totalHeightDp - 16.dp) / alphabet.size).value
            kotlin.math.ceil(15f / perLetter.coerceAtLeast(1f)).toInt().coerceAtLeast(1)
        }
        val shown = remember(alphabet, step) { alphabet.filterIndexed { i, _ -> i % step == 0 } }
        val itemHeightDp = (totalHeightDp - 16.dp) / shown.size

        val touchYDp = with(density) { touchY.toDp() }
        // NOTE: A landscape rail can be shorter than 72dp, so the upper bound never drops below the lower bound.
        val targetY = (8.dp + touchYDp - 28.dp).value.coerceIn(8f, maxOf(8f, (totalHeightDp - 64.dp).value))
        val animatedBubbleY by animateFloatAsState(
            targetValue = targetY,
            animationSpec = spring(stiffness = 1200f),
            label = "BubbleY"
        )

        // Floating preview bubble tracking finger position
        AnimatedVisibility(
            visible = isDragging && (activeLetter != null || lastLetter != null),
            enter = scaleIn(
                initialScale = 0.65f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = 1200f
                )
            ) + fadeIn(tween(140)),
            exit = scaleOut(
                targetScale = 0.8f,
                animationSpec = tween(200)
            ) + fadeOut(tween(200)),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-72).dp, y = animatedBubbleY.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shadowElevation = 8.dp,
                tonalElevation = 4.dp,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = (activeLetter ?: lastLetter)?.toString().orEmpty(),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Alphabet rail pinned to right edge with 4dp gesture safety padding
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp)
                .width(32.dp)
                .fillMaxHeight()
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .dcSystemGestureExclusion()
                .semantics { contentDescription = railDescription }
                .pointerInput(shown, totalHeightPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val currentHeight = size.height
                        if (currentHeight > 0) {
                            hideJob?.cancel()
                            val clampedY = down.position.y.coerceIn(0f, currentHeight.toFloat())
                            touchY = clampedY
                            isDragging = true
                            val dynamicItemHeight = currentHeight.toFloat() / shown.size
                            val index = (clampedY / dynamicItemHeight).toInt().coerceIn(0, shown.lastIndex)
                            val letter = shown[index]
                            if (activeLetter != letter) {
                                activeLetter = letter
                                lastLetter = letter
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onLetterSelected(letter)
                            }
                        }
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) {
                                hideJob?.cancel()
                                hideJob = scope.launch {
                                    delay(300L)
                                    isDragging = false
                                    activeLetter = null
                                }
                                break
                            }
                            val currentHeight = size.height
                            if (currentHeight > 0) {
                                val clampedY = change.position.y.coerceIn(0f, currentHeight.toFloat())
                                touchY = clampedY
                                val dynamicItemHeight = currentHeight.toFloat() / shown.size
                                val index = (clampedY / dynamicItemHeight).toInt().coerceIn(0, shown.lastIndex)
                                val letter = shown[index]
                                if (activeLetter != letter) {
                                    activeLetter = letter
                                    lastLetter = letter
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onLetterSelected(letter)
                                }
                            }
                            change.consume()
                        }
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // NOTE: Sized in dp, so a large system font cannot grow the letters past their slot.
            val dynamicFontSize = with(density) { (itemHeightDp.value * 0.70f).coerceIn(9f, 13.5f).dp.toSp() }

            shown.forEach { letter ->
                val isSelected = activeLetter == letter
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeightDp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = letter.toString(),
                        fontSize = if (isSelected) dynamicFontSize * 1.25f else dynamicFontSize,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        style = LocalTextStyle.current.copy(
                            lineHeight = if (isSelected) dynamicFontSize * 1.25f else dynamicFontSize,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                }
            }
        }
    }
}
