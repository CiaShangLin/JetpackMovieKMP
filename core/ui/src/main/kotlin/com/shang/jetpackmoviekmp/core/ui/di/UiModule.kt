package com.shang.jetpackmoviekmp.core.ui.di

import coil3.ImageLoader
import coil3.request.crossfade
import com.shang.jetpackmoviekmp.core.ui.coil.HostInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * 提供 Android UI module 所需的 Coil interceptor 與 [ImageLoader]。
 *
 * @return 可安裝到 Android Koin bootstrap 的 module。
 */
fun uiModule() = module {
    single { HostInterceptor(get()) }
    single {
        ImageLoader.Builder(androidContext())
            .components {
                add(get<HostInterceptor>())
            }
            .crossfade(true)
            .build()
    }
}
