package com.nitinpatil.model3dviewer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.nitinpatil.model3dviewer.ui.screens.ViewerScreen
import com.nitinpatil.model3dviewer.ui.theme.Model3DViewerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Model3DViewerTheme {
                ViewerScreen()
            }
        }
    }
}
