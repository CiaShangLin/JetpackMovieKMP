package com.shang.jetpackmoviekmp.network.provider

import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultLanguageProviderTest {

    @Test
    fun getLanguageCode_returns_traditional_chinese() {
        val provider = DefaultLanguageProvider()

        assertEquals("zh-TW", provider.getLanguageCode())
    }
}
