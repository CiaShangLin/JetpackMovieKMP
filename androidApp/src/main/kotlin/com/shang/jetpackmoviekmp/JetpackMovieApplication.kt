package com.shang.jetpackmoviekmp

import android.app.Application
import android.content.pm.ApplicationInfo
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.shang.jetpackmoviekmp.core.ui.di.uiModule
import com.shang.jetpackmoviekmp.di.mainModule
import org.koin.android.ext.android.inject
import org.koin.core.context.loadKoinModules

class JetpackMovieApplication : Application(), SingletonImageLoader.Factory {

    private val _imageLoader by inject<ImageLoader>()

    override fun onCreate() {
        super.onCreate()
        val isDebug = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        initKoinAndroid(context = this, isDebug = isDebug)
        loadKoinModules(listOf(uiModule(), mainModule()))
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return _imageLoader
    }
}
