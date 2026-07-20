package com.shang.jetpackmoviekmp

import android.app.Application
import android.content.pm.ApplicationInfo
import com.shang.jetpackmoviekmp.core.ui.di.uiModule
import org.koin.core.context.loadKoinModules

class JetpackMovieApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val isDebug = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        initKoinAndroid(context = this, isDebug = isDebug)
        loadKoinModules(uiModule())
    }
}
