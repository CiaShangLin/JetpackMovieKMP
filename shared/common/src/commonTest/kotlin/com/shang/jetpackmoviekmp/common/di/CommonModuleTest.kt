package com.shang.jetpackmoviekmp.common.di

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.test.KoinTest
import org.koin.test.inject
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull

class CommonModuleTest : KoinTest {

    private val coroutineScope: CoroutineScope by inject()
    private val ioDispatcher: CoroutineDispatcher by inject(qualifier = named(CommonDispatcher.IO))

    @BeforeTest
    fun setUp() {
        startKoin {
            modules(commonModule())
        }
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun commonModule_resolves_coroutineScope() {
        assertNotNull(coroutineScope)
    }

    @Test
    fun commonModule_resolves_io_coroutineDispatcher() {
        assertNotNull(ioDispatcher)
    }
}
