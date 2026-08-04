package com.shang.jetpackmoviekmp.network.di

import com.shang.jetpackmoviekmp.common.LanguageProvider
import com.shang.jetpackmoviekmp.network.datasource.MovieDataSource
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull

class NetworkModuleTest : KoinTest {

    private val movieDataSource: MovieDataSource by inject()

    @BeforeTest
    fun setUp() {
        startKoin {
            modules(
                module {
                    single<LanguageProvider> {
                        object : LanguageProvider {
                            override fun getLanguageCode(): String = "zh-TW"
                        }
                    }
                },
                networkModule(isDebug = true),
            )
        }
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun networkModule_resolves_MovieDataSource() {
        assertNotNull(movieDataSource)
    }
}
