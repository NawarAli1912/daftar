package com.daftar.app.store

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

// App-wide row gesture (the standard): swipe a row sideways to reveal a red «حذف» button —
// tapping it soft-deletes (voids) the row. Tapping the row itself still opens its detail.
// One interaction model everywhere. Content must be opaque so the button hides when closed.
@Composable
internal fun SwipeRow(onTap: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val revealPx = with(LocalDensity.current) { 96.dp.toPx() }
    val ambientDir = LocalLayoutDirection.current
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val origin = LocalSheetOrigin.current
    var bounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    // Positioning runs in a forced-LTR space so the mechanics are deterministic regardless of
    // the app's layout direction: the content always slides LEFT to reveal the «حذف» button
    // pinned on the physical RIGHT. Content restores the real direction so Arabic still lays
    // out correctly.
    androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(modifier.fillMaxWidth()) {
            Box(Modifier.matchParentSize(), contentAlignment = Alignment.CenterEnd) {
                Box(
                    Modifier.width(96.dp).fillMaxHeight().clip(RoundedCornerShape(rMd)).background(cDebt)
                        .tap { scope.launch { offsetX.animateTo(0f) }; onDelete() },
                    contentAlignment = Alignment.Center,
                ) { Text("حذف", fontSize = fBody, fontWeight = FontWeight.Bold, color = cAink) }
            }
            Box(
                Modifier
                    .onGloballyPositioned { bounds = it.boundsInWindow() }
                    .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                    .background(cCard)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                scope.launch {
                                    val target = if (offsetX.value < -revealPx / 2f) -revealPx else 0f
                                    offsetX.animateTo(target, spring(dampingRatio = 0.85f, stiffness = 400f))
                                }
                            },
                        ) { change, drag ->
                            change.consume()
                            scope.launch { offsetX.snapTo((offsetX.value + drag).coerceIn(-revealPx, 0f)) }
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures {
                            if (abs(offsetX.value) > 1f) scope.launch { offsetX.animateTo(0f) } // tap closes an open row
                            else { origin.value = bounds; onTap() }
                        }
                    },
            ) {
                androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides ambientDir) { content() }
            }
        }
    }
}
