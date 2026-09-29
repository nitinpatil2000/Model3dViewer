package com.nitinpatil.model3dviewer.data.glb

import android.content.Context

object GlbCatalog {
    fun load(context: Context): List<GlbItem> =
        context.assets.list("models").orEmpty()
            .filter { it.endsWith(".glb", ignoreCase = true) }
            .sorted()
            .map { GlbItem("models/$it", it.substringBeforeLast('.')) }
}