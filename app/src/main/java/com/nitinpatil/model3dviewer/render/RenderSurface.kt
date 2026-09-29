package com.nitinpatil.model3dviewer.render

import android.util.Log
import android.view.Choreographer
import android.view.Surface
import android.view.SurfaceView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.filament.EntityManager
import com.google.android.filament.Renderer
import com.google.android.filament.Skybox
import com.google.android.filament.SwapChain
import com.google.android.filament.View
import com.google.android.filament.Viewport
import com.google.android.filament.android.UiHelper
import com.nitinpatil.model3dviewer.data.model.ModelInstance

class RenderSurface(
    private val h: EngineHolder
) : Choreographer.FrameCallback{

    private val uiHelper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK)
    private val choreographer = Choreographer.getInstance()

    private var swapChain: SwapChain? = null
    private var running = false

    val scenes = LinkedHashMap<String, ModalScene>()
    private var current: List<ModelInstance> = emptyList()
    private var surfaceW = 0
    private var surfaceH = 0

    private val bgScene = h.engine.createScene()
    private val bgView = h.engine.createView()
    private val bgCamEntity = EntityManager.get().create()
    private val bgCamera = h.engine.createCamera(bgCamEntity)
    private val bgSkybox = Skybox.Builder().color(0.09f, 0.09f, 0.11f, 1f).build(h.engine)

    var labelPositions by mutableStateOf<Map<String, List<ModalScene.LabelPos>>>(emptyMap())
        private set

    private var destroyed = false

    init {
        bgView.scene = bgScene
        bgView.camera = bgCamera
        bgView.isPostProcessingEnabled = false
        bgView.antiAliasing = View.AntiAliasing.NONE
        bgScene.skybox = bgSkybox
    }


    fun attach(surfaceView: SurfaceView) {
        uiHelper.renderCallback = object : UiHelper.RendererCallback {
            override fun onNativeWindowChanged(surface: Surface) {
                swapChain?.let { h.engine.destroySwapChain(it) }
                swapChain = h.engine.createSwapChain(surface, uiHelper.swapChainFlags)
            }
            override fun onDetachedFromSurface() {
                swapChain?.let { h.engine.destroySwapChain(it); h.engine.flushAndWait() }
                swapChain = null
            }
            override fun onResized(width: Int, height: Int) {
                surfaceW = width; surfaceH = height
                bgView.viewport = Viewport(0, 0, width, height)
                sync(current)
            }
        }
        uiHelper.attachTo(surfaceView)
        start()
    }

    fun sync(list: List<ModelInstance>) {
        current = list
        val ids = list.map { it.id }.toSet()
        scenes.keys.filter { it !in ids }.forEach { scenes.remove(it)?.destroy() }   // closed models free
        if (surfaceH == 0) return
        list.forEach { m ->
            val s = scenes.getOrPut(m.id) { ModalScene(h, m.glbPath) }
            val r = m.rect
            // Compose: top-left origin | Filament: bottom-left origin -> y flip
            s.setViewport(
                r.left.toInt(),
                surfaceH - r.bottom.toInt(),
                r.width.toInt(),
                r.height.toInt()
            )
        }
    }

    fun start() { if (!running) { running = true; choreographer.postFrameCallback(this) } }
    fun stop() { running = false; choreographer.removeFrameCallback(this) }


    override fun doFrame(frameTimeNanos: Long) {
        if (!running) return
        choreographer.postFrameCallback(this)
        updateLabels()

        val sc = swapChain
        if (uiHelper.isReadyToRender && sc != null && h.renderer.beginFrame(sc, frameTimeNanos)) {
            h.renderer.render(bgView)
            scenes.values.forEach { h.renderer.render(it.view) }   // models (add order = z order)
            h.renderer.endFrame()
        }
    }


    //zoom and rotate 3d container
    fun rotate(id: String, dx: Float, dy: Float) { scenes[id]?.rotate(dx, dy) }
    fun zoom(id: String, factor: Float) { scenes[id]?.zoom(factor) }


    private fun updateLabels() {
        val prev = labelPositions
        var changed = false
        val next = HashMap<String, List<ModalScene.LabelPos>>()
        for (m in current) {
            val s = scenes[m.id] ?: continue
            if (!m.labelsVisible) { if (m.id in prev) changed = true; continue }
            val old = prev[m.id]
            if (s.labelsDirty || old == null) {
                next[m.id] = s.projectLabels(m.rect)
                changed = true
            } else next[m.id] = old
        }

        if (prev.keys != next.keys) changed = true
        if (changed) {
            Log.d("LabelPos", next.values.firstOrNull()?.firstOrNull().toString())
            labelPositions = next
        }
    }

    fun destroy() {
        if (destroyed) return
        destroyed = true
        stop()
        uiHelper.detach()
        h.engine.flushAndWait()

        scenes.values.forEach { it.destroy() }
        scenes.clear()

        bgScene.skybox = null
        h.engine.destroySkybox(bgSkybox)
        h.engine.destroyView(bgView)
        h.engine.destroyScene(bgScene)
        h.engine.destroyCameraComponent(bgCamEntity)
        EntityManager.get().destroy(bgCamEntity)
    }
}
