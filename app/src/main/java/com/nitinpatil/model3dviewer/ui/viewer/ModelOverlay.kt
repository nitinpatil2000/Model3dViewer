package com.nitinpatil.model3dviewer.ui.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nitinpatil.model3dviewer.data.model.InteractionMode
import com.nitinpatil.model3dviewer.data.model.ModelInstance
import kotlin.math.roundToInt

@Composable
fun ModelOverlay(
    m: ModelInstance,
    onMove: (Float, Float) -> Unit,
    onResize: (Float) -> Unit,
    onToggleMode: () -> Unit,
    onToggleLabels: () -> Unit,
    onRotate: (Float, Float) -> Unit,
    onZoom: (Float) -> Unit,
    onClose: () -> Unit
) {
    val density = LocalDensity.current
    val active = m.mode == InteractionMode.INTERACTION

    val modeState = rememberUpdatedState(m.mode)
    val moveCb = rememberUpdatedState(onMove)
    val resizeCb = rememberUpdatedState(onResize)
    val rotateCb = rememberUpdatedState(onRotate)
    val zoomCb = rememberUpdatedState(onZoom)

    Box(
        Modifier
            .offset { IntOffset(m.rect.left.roundToInt(), m.rect.top.roundToInt()) }
            .size(
                with(density) { m.rect.width.toDp() },
                with(density) { m.rect.height.toDp() })
            .border(2.dp, if (active) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.5f))
            .pointerInput(m.id) {
                awaitEachGesture {
                    awaitFirstDown()
                    val mode = modeState.value
                    do {
                        val event = awaitPointerEvent()
                        val down = event.changes.filter { it.pressed }

                        when (mode) {
                            InteractionMode.NORMAL -> {
                                if (down.size == 1) {
                                    val c = down[0]
                                    val dx = c.position.x - c.previousPosition.x
                                    val dy = c.position.y - c.previousPosition.y
                                    if (dx != 0f || dy != 0f) moveCb.value(dx, dy)
                                } else if (down.size >= 2) {
                                    val z = event.calculateZoom()
                                    if (z != 1f) resizeCb.value(z)
                                }
                            }
                            InteractionMode.INTERACTION -> {
                                if (down.size == 1) {
                                    val c = down[0]
                                    val dx = c.position.x - c.previousPosition.x
                                    val dy = c.position.y - c.previousPosition.y
                                    if (dx != 0f || dy != 0f) rotateCb.value(dx, dy)
                                } else if (down.size >= 2) {
                                    val z = event.calculateZoom()
                                    if (z != 1f) zoomCb.value(z)
                                }
                            }
                        }

                        event.changes.forEach { if (it.position != it.previousPosition) it.consume() }


                    } while (event.changes.any { it.pressed })
                }
            }
            ) {
        Row(
            Modifier.align(Alignment.TopEnd).padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            OverlayButton("⟳", active, onToggleMode)
            OverlayButton("Aa", m.labelsVisible, onToggleLabels)
            OverlayButton("✕", false, onClose)
        }
    }
}

@Composable
private fun OverlayButton(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(if (selected) Color(0xFF4CAF50) else Color.Black.copy(alpha = 0.6f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(text, color = Color.White, fontSize = 12.sp) }
}