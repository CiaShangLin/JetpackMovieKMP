package com.shang.jetpackmoviekmp.database.entity

import com.shang.jetpackmoviekmp.model.MovieCardResult
import kotlin.test.Test
import kotlin.test.assertEquals

class MovieCollectEntityTest {

    @Test
    fun asExtendedModel_maps_fields_and_marks_isCollect_true() {
        val entity = MovieCollectEntity(
            id = 42,
            title = "Inception",
            posterPath = "/poster.jpg",
            voteAverage = 8.8,
            releaseDate = "2010-07-16",
            timestamp = 1_000L,
        )

        val result = entity.asExtendedModel()

        assertEquals(
            MovieCardResult(
                id = 42,
                title = "Inception",
                posterPath = "/poster.jpg",
                voteAverage = 8.8,
                releaseDate = "2010-07-16",
                timestamp = 1_000L,
                isCollect = true,
            ),
            result,
        )
    }
}
