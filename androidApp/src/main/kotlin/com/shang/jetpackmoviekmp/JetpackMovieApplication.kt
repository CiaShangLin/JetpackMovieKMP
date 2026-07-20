package com.shang.jetpackmoviekmp

import android.app.Application
import android.content.pm.ApplicationInfo
import com.shang.jetpackmoviekmp.core.ui.di.uiModule
import com.shang.jetpackmoviekmp.database.getDatabaseBuilder
import com.shang.jetpackmoviekmp.datastore.createUserPreferencesDataStore
import org.koin.android.ext.koin.androidContext

class JetpackMovieApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val isDebug = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        initKoin(
            dataStore = createUserPreferencesDataStore(this),
            databaseBuilder = { getDatabaseBuilder(this) },
            isDebug = isDebug,
        ) {
            androidContext(this@JetpackMovieApplication)
            modules(uiModule())
        }
    }
}
