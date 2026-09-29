package com.nitinpatil.model3dviewer.data.model

import androidx.compose.ui.geometry.Rect
import java.util.UUID

enum class InteractionMode {
    NORMAL,
    INTERACTION
}

data class ModelInstance(
    val id: String = UUID.randomUUID().toString(),
    val glbPath: String,
    val name: String,
    val rect: Rect,
    val mode: InteractionMode = InteractionMode.NORMAL,
    val labelsVisible: Boolean = false
)