package com.nitinpatil.model3dviewer.data.glb

import android.content.Context
import android.util.Log
import com.nitinpatil.model3dviewer.data.model.PartLabel
import org.json.JSONObject
import java.io.DataInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object GlbLabelParser {
    private const val MAGIC_GLTF = 0x46546C67       // "glTF"
    private const val CHUNK_JSON = 0x4E4F534A       // "JSON"

    fun parse(context: Context, assetPath: String): List<PartLabel> = try {
        context.assets.open(assetPath).use { raw ->
            val input = DataInputStream(raw.buffered())
            val head = ByteArray(20).also { input.readFully(it) }
            val bb = ByteBuffer.wrap(head).order(ByteOrder.LITTLE_ENDIAN)

            if (bb.getInt(0) != MAGIC_GLTF || bb.getInt(16) != CHUNK_JSON) return emptyList()

            val jsonBytes = ByteArray(bb.getInt(12)).also { input.readFully(it) }
            val nodes = JSONObject(String(jsonBytes, Charsets.UTF_8)).optJSONArray("nodes")
                ?: return emptyList()

            buildList {
                for (i in 0 until nodes.length()) {
                    val node = nodes.getJSONObject(i)
                    val text = node.optJSONObject("extras")?.optString("prop", "").orEmpty()
                    if (text.isNotBlank()) {
                        add(PartLabel(node.optString("name", ""), text))
                    }
                }
            }
        }
    } catch (e: Exception) {
        Log.e("GlbLabelParser", "Parse failed: $assetPath", e)
        emptyList()
    }
}