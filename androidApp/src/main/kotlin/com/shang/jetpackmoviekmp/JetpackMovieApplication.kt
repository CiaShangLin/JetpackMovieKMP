package com.shang.jetpackmoviekmp

import android.app.Application
import android.content.pm.ApplicationInfo
import com.shang.jetpackmoviekmp.network.di.networkModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class JetpackMovieApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val isDebug = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        startKoin {
            androidContext(this@JetpackMovieApplication)
            modules(networkModule(isDebug))
        }
    }
}
