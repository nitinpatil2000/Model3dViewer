package com.nitinpatil.model3dviewer.ui.viewer

import android.graphics.Canvas
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nitinpatil.model3dviewer.data.model.ModelInstance
import com.nitinpatil.model3dviewer.render.ModalScene
import kotlin.collections.filter

@Composable
fun LabelOverlay(
    models: List<ModelInstance>,
    positions: () -> Map<String, List<ModalScene.LabelPos>>
) {
    val measurer = rememberTextMeasurer()
    val style = remember { TextStyle(color = Color.White, fontSize = 11.sp) }

    Canvas(Modifier.fillMaxSize()) {
        val map = positions()
        val pad = 6.dp.toPx()
        val gap = 2.dp.toPx()

        for (m in models) {
            if (!m.labelsVisible) continue
            val list = map[m.id]?.filter { it.visible } ?: continue
            val r = m.rect

            clipRect(r.left, r.top, r.right, r.bottom) {
                val items = list.map { it to measurer.measure(it.text, style) }
                val (left, right) = items.partition { it.first.x < r.center.x }

                fun drawSide(
                    side: List<Pair<ModalScene.LabelPos, TextLayoutResult>>,
                    isLeft: Boolean
                ) {
                    var prevBottom = r.top
                    for ((p, t) in side.sortedBy { it.first.y }) {
                        val w = t.size.width.toFloat()
                        val h = t.size.height.toFloat()
                        val boxW = w + pad * 2
                        val boxH = h + pad
                        val boxX = if (isLeft) r.left + pad else r.right - pad - boxW
                        val boxY = maxOf(p.y - boxH / 2, prevBottom + gap)
                            .coerceIn(r.top, maxOf(r.top, r.bottom - boxH))
                        prevBottom = boxY + boxH

                        val lineEndX = if (isLeft) boxX + boxW else boxX
                        val lineEndY = boxY + boxH / 2

                        drawLine(
                            Color.White.copy(alpha = 0.8f),
                            Offset(p.x, p.y),
                            Offset(lineEndX, lineEndY),
                            1.5.dp.toPx()
                        )
                        drawCircle(Color(0xFF4CAF50), 3.dp.toPx(), Offset(p.x, p.y))
                        drawRoundRect(
                            Color.Black.copy(alpha = 0.7f), Offset(boxX, boxY), Size(boxW, boxH),
                            CornerRadius(4.dp.toPx())
                        )
                        drawText(t, topLeft = Offset(boxX + pad, boxY + pad / 2))
                    }
                }
                drawSide(left, true)
                drawSide(right, false)
            }
        }
    }
}