package com.mohitarora.dualclone.ui.screen.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout

/**
 * Header that slides away when the list scrolls up and returns as soon as it scrolls down.
 * Used on short screens such as a phone in landscape, where the filters would otherwise leave room for one row.
 */
@Composable
fun CollapsingHeaderLayout(
    header: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    body: @Composable () -> Unit
) {
    var headerHeight by remember { mutableFloatStateOf(0f) }
    var offset by remember { mutableFloatStateOf(0f) }
    val connection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val next = (offset + available.y).coerceIn(-headerHeight, 0f)
                val consumed = next - offset
                offset = next
                return Offset(0f, consumed)
            }
        }
    }
    Column(modifier = modifier.fillMaxSize().clipToBounds().nestedScroll(connection)) {
        Column(
            modifier = Modifier.layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                headerHeight = placeable.height.toFloat()
                val visible = (placeable.height + offset).toInt().coerceAtLeast(0)
                layout(placeable.width, visible) { placeable.place(0, offset.toInt()) }
            }
        ) { header() }
        body()
    }
}
