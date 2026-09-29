package com.nitinpatil.model3dviewer

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App : Application(){
    override fun onCreate() {
        super.onCreate()
        //initialize filament + gltfio
        com.google.android.filament.utils.Utils.init()
    }
}