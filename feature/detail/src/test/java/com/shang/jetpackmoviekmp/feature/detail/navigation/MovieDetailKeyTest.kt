package com.shang.jetpackmoviekmp.feature.detail.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

class MovieDetailKeyTest {

    @Test
    fun `key preserves movie id`() {
        assertEquals(42, MovieDetailKey(42).movieId)
    }
}
