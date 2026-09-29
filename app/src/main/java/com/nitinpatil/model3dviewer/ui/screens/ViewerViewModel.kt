package com.nitinpatil.model3dviewer.ui.screens

import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.ViewModel
import com.nitinpatil.model3dviewer.data.glb.GlbCatalog
import com.nitinpatil.model3dviewer.data.glb.GlbItem
import com.nitinpatil.model3dviewer.data.model.InteractionMode
import com.nitinpatil.model3dviewer.data.model.ModelInstance
import com.nitinpatil.model3dviewer.render.EngineHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class ViewerViewModel @Inject constructor(
    val engineHolder: EngineHolder
) : ViewModel() {

    private val _models = MutableStateFlow<List<ModelInstance>>(emptyList())
    val models = _models.asStateFlow()

    val catalog: List<GlbItem> = GlbCatalog.load(engineHolder.context)

    fun addModel(glbPath: String, name: String, screenW: Float, screenH: Float) {
        val size = minOf(screenW, screenH) * 0.5f
        val top0 = 100f
        val step = 80f

        val maxOffset = minOf(screenW - size, screenH - size - top0).coerceAtLeast(0f)


        val steps = (maxOffset / step).toInt() + 1
        val offset = (_models.value.size % steps) * step
        val rect = Rect(offset, top0 + offset, offset + size, top0 + offset + size)


        _models.update { it + ModelInstance(glbPath = glbPath, name = name, rect = rect) }

    }


    fun remove(id: String) = _models.update { list ->
        list.filterNot { it.id == id }
    }

    fun toggleMode(id: String) = update(id) {
        it.copy(
            mode = if (it.mode == InteractionMode.NORMAL) {
                InteractionMode.INTERACTION
            } else {
                InteractionMode.NORMAL
            }
        )
    }


    fun toggleLabels(id: String) = update(id) { it.copy(labelsVisible = !it.labelsVisible) }

    private fun update(id: String, block: (ModelInstance) -> ModelInstance) {
        _models.update { list ->
            list.map {
                if (it.id == id) {
                    block(it)
                } else {
                    it
                }
            }
        }
    }

    fun move(id: String, dx: Float, dy: Float, screenW: Float, screenH: Float) = update(id) {
        it.copy(rect = it.rect.translate(dx, dy).clampInside(screenW, screenH))
    }

    fun resize(id: String, factor: Float, screenW: Float, screenH: Float) = update(id) {
        val minSide = minOf(screenW, screenH)
        val newSize = (it.rect.width * factor).coerceIn(minSide * 0.25f, minSide) // min/max limit
        val c = it.rect.center
        val r = Rect(c.x - newSize / 2, c.y - newSize / 2, c.x + newSize / 2, c.y + newSize / 2)
        it.copy(rect = r.clampInside(screenW, screenH))
    }

    private fun Rect.clampInside(w: Float, h: Float): Rect {
        val dx = when { left < 0f -> -left; right > w -> w - right; else -> 0f }
        val dy = when { top < 0f -> -top; bottom > h -> h - bottom; else -> 0f }
        return translate(dx, dy)
    }

    override fun onCleared() = engineHolder.destroy()
}