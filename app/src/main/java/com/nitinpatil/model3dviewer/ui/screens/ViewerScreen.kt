package com.nitinpatil.model3dviewer.ui.screens

import android.view.SurfaceView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.nitinpatil.model3dviewer.render.RenderSurface
import com.nitinpatil.model3dviewer.ui.viewer.LabelOverlay
import com.nitinpatil.model3dviewer.ui.viewer.ModelOverlay

@Composable
fun ViewerScreen(vm: ViewerViewModel = hiltViewModel()) {
    val surface = remember { RenderSurface(vm.engineHolder) }
    val models by vm.models.collectAsState()
    var screenSize by remember { mutableStateOf(IntSize.Zero) }

    var menuOpen by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            surface.destroy()
        }
    }
    SideEffect { surface.sync(models) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { screenSize = it }
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                SurfaceView(ctx).also {
                    surface.attach(it)
                }
            }
        )

        LabelOverlay(models = models, positions = { surface.labelPositions })

        models.forEach { m ->
            key(m.id) {
                ModelOverlay(
                    m,
                    onMove = { dx, dy ->
                        vm.move(
                            m.id,
                            dx,
                            dy,
                            screenSize.width.toFloat(),
                            screenSize.height.toFloat()
                        )
                    },
                    onResize = { f ->
                        vm.resize(
                            m.id,
                            f,
                            screenSize.width.toFloat(),
                            screenSize.height.toFloat()
                        )
                    },
                    onRotate = { dx, dy -> surface.rotate(m.id, dx, dy) },
                    onZoom = { f -> surface.zoom(m.id, f) },
                    onToggleMode = { vm.toggleMode(m.id) },
                    onToggleLabels = { vm.toggleLabels(m.id) },
                    onClose = { vm.remove(m.id) }
                )
            }
        }


        Box(Modifier.align(Alignment.BottomCenter).padding(16.dp)) {
            Button(onClick = { menuOpen = true }) { Text("Add Model") }

            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                vm.catalog.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item.name) },
                        onClick = {
                            menuOpen = false
                            vm.addModel(
                                item.path, item.name,
                                screenSize.width.toFloat(), screenSize.height.toFloat()
                            )
                        }
                    )
                }
            }
        }
    }
}
