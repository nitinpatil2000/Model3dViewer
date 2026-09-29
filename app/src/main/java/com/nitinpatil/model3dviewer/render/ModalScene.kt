package com.nitinpatil.model3dviewer.render

import android.util.Log
import com.google.android.filament.Camera
import com.google.android.filament.EntityManager
import com.google.android.filament.IndirectLight
import com.google.android.filament.LightManager
import com.google.android.filament.Scene
import com.google.android.filament.Skybox
import com.google.android.filament.View
import com.google.android.filament.Viewport
import com.google.android.filament.gltfio.FilamentAsset
import com.google.android.filament.utils.Float2
import com.nitinpatil.model3dviewer.data.glb.GlbLabelParser
import com.nitinpatil.model3dviewer.data.model.PartLabel
import kotlin.math.cos
import kotlin.math.sin

class ModalScene(
    private val h: EngineHolder, glbPath: String
) {

    private val engine = h.engine
    val scene: Scene = engine.createScene()
    val view: View = engine.createView()

    private val cameraEntity = EntityManager.get().create()
    val camera: Camera = engine.createCamera(cameraEntity)

    private val lightEntity = EntityManager.get().create()
    private val asset: FilamentAsset? = h.loadGlb(glbPath)

    private val skybox = Skybox.Builder().color(0.13f, 0.13f, 0.16f, 1f).build(engine)
    private val ibl = IndirectLight.Builder()
        .irradiance(1, floatArrayOf(1f, 1f, 1f))   // constant ambient light
        .intensity(30_000f)
        .build(engine)

    private var yaw = 0f          // degrees
    private var pitch = 0f        // degrees
    private var distance = 3f

    val labels: List<PartLabel> = GlbLabelParser.parse(h.context, glbPath)

    data class LabelPos(val text: String, val x: Float, val y: Float, val visible: Boolean)
    private val labelEntities: List<Pair<PartLabel, Int>> = labels.mapNotNull { l ->
        val e = asset?.getFirstEntityByName(l.nodeName) ?: 0
        if (e != 0) l to e else null
    }
    private val worldM = FloatArray(16)
    private val viewM = DoubleArray(16)
    private val projM = DoubleArray(16)
    var labelsDirty = true
    private var destroyed = false


    init {
        Log.d("Labels", "$glbPath -> $labels")
        view.scene = scene
        view.camera = camera

        scene.skybox = skybox
        scene.indirectLight = ibl

        view.antiAliasing = View.AntiAliasing.NONE //for low end
        view.dithering = View.Dithering.NONE
        view.isPostProcessingEnabled = true //bloom, SSAO, tone-mapping off -- low end devices

        view.bloomOptions = View.BloomOptions().apply { enabled = false }
        view.ambientOcclusionOptions = View.AmbientOcclusionOptions().apply { enabled = false }


        asset?.let {
            scene.addEntities(it.entities)
            fitToUnitCube(it)
        }


        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(1f, 1f, 1f)
            .intensity(110_00f)
            .direction(0f, -1f, -1f)
            .build(engine, lightEntity)
        scene.addEntity(lightEntity)

        updateCamera()

    }

    private fun fitToUnitCube(a: FilamentAsset){
        val box = a.boundingBox
        val c = box.center
        val hx = box.halfExtent
        val s = 1f / maxOf(hx[0], hx[1], hx[2])

        val tm = engine.transformManager
        tm.setTransform(
            tm.getInstance(a.root),
            floatArrayOf(
                s, 0f, 0f, 0f,
                0f, s, 0f, 0f,
                0f, 0f, s, 0f,
                -c[0] * s, -c[1] * s, -c[2] * s, 1f
            )
        )
    }

    //responsive ui
    fun setViewport(x: Int, y: Int, w: Int, h: Int) {
        view.viewport = Viewport(x, y, w, h)
        if (h > 0 && w > 0) {
            camera.setProjection(45.0, w.toDouble() / h, 0.1, 100.0, Camera.Fov.VERTICAL)
        }
        labelsDirty = true
    }

    fun rotate(dx: Float, dy: Float) {
        yaw -= dx * 0.4f
        pitch = (pitch + dy * 0.4f).coerceIn(-89f, 89f)
        updateCamera()
        labelsDirty = true

    }

    fun zoom(factor: Float) {
        distance = (distance / factor).coerceIn(1.5f, 8f)
        updateCamera()
        labelsDirty = true
    }

    private fun updateCamera() {
        val y = Math.toRadians(yaw.toDouble())
        val p = Math.toRadians(pitch.toDouble())
        camera.lookAt(
            distance * cos(p) * sin(y), distance * sin(p), distance * cos(p) * cos(y),
            0.0, 0.0, 0.0,
            0.0, 1.0, 0.0
        )
    }

    fun projectLabels(rect: androidx.compose.ui.geometry.Rect): List<LabelPos> {
        camera.getViewMatrix(viewM)
        camera.getProjectionMatrix(projM)
        val tm = engine.transformManager
        labelsDirty = false

        return labelEntities.map { (label, entity) ->
            val inst = tm.getInstance(entity)
            tm.getWorldTransform(inst, worldM)           // column-major, translation = [12],[13],[14]
            val x = worldM[12].toDouble()
            val y = worldM[13].toDouble()
            val z = worldM[14].toDouble()

            // v = View * p
            val vx = viewM[0] * x + viewM[4] * y + viewM[8] * z + viewM[12]
            val vy = viewM[1] * x + viewM[5] * y + viewM[9] * z + viewM[13]
            val vz = viewM[2] * x + viewM[6] * y + viewM[10] * z + viewM[14]
            // clip = Proj * v   (w input = 1)
            val cx = projM[0] * vx + projM[4] * vy + projM[8] * vz + projM[12]
            val cy = projM[1] * vx + projM[5] * vy + projM[9] * vz + projM[13]
            val cw = projM[3] * vx + projM[7] * vy + projM[11] * vz + projM[15]

            if (cw <= 1e-6) {
                LabelPos(label.text, 0f, 0f, false)
            } else {
                val ndcX = cx / cw
                val ndcY = cy / cw
                LabelPos(
                    label.text,
                    rect.left + ((ndcX * 0.5 + 0.5) * rect.width).toFloat(),
                    rect.top + ((1.0 - (ndcY * 0.5 + 0.5)) * rect.height).toFloat(),
                    true
                )
            }
        }
    }

    fun destroy() {
        if (destroyed) return
        destroyed = true

        asset?.let {
            scene.removeEntities(it.entities)
            h.destroyAsset(it)
        }
        scene.removeEntity(lightEntity)
        scene.skybox = null
        scene.indirectLight = null
        engine.destroySkybox(skybox)
        engine.destroyIndirectLight(ibl)

        engine.destroyEntity(lightEntity)
        EntityManager.get().destroy(lightEntity)
        engine.destroyCameraComponent(cameraEntity)
        EntityManager.get().destroy(cameraEntity)

        engine.destroyView(view)
        engine.destroyScene(scene)
    }
}