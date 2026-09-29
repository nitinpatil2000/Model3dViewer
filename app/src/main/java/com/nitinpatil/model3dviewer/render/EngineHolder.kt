package com.nitinpatil.model3dviewer.render

import android.content.Context
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.Renderer
import com.google.android.filament.gltfio.AssetLoader
import com.google.android.filament.gltfio.FilamentAsset
import com.google.android.filament.gltfio.ResourceLoader
import com.google.android.filament.gltfio.UbershaderProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EngineHolder @Inject constructor(
  @ApplicationContext val context: Context
){
    val engine: Engine = Engine.create()
    val renderer: Renderer = engine.createRenderer()

    private val materialProvider = UbershaderProvider(engine)
    private val assetLoader = AssetLoader(engine, materialProvider, EntityManager.get())
    private val resourceLoader = ResourceLoader(engine)
    private val bytesCache = HashMap<String, ByteBuffer>()

    fun loadGlb(path: String): FilamentAsset? {
        val buffer = bytesCache.getOrPut(path){
            val bytes = context.assets.open(path).use { it.readBytes() }
            ByteBuffer.allocateDirect(bytes.size).put(bytes).apply { flip() }
        }

        buffer.rewind()
        return assetLoader.createAsset(buffer)?.also {
            resourceLoader.loadResources(it)
            it.releaseSourceData()
        }
    }

    fun destroyAsset(asset: FilamentAsset) = assetLoader.destroyAsset(asset)

    fun destroy(){
        resourceLoader.destroy()
        assetLoader.destroy()
        materialProvider.destroyMaterials()
        materialProvider.destroy()
        engine.destroyRenderer(renderer)
        engine.destroy()
    }
}