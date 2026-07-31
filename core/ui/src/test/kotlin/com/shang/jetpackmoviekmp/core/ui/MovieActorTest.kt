package com.shang.jetpackmoviekmp.core.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MovieActorTest {

    @Test
    fun `blank path uses placeholder model`() {
        assertNull("".asActorImageModel())
    }

    @Test
    fun `image path remains unchanged`() {
        assertEquals("/profile.jpg", "/profile.jpg".asActorImageModel())
    }
}
