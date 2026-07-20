package com.shang.jetpackmoviekmp.database.entity

import com.shang.jetpackmoviekmp.model.MovieCardResult
import kotlin.test.Test
import kotlin.test.assertEquals

class MovieHistoryEntityTest {

    @Test
    fun asExtendedModel_maps_fields_and_leaves_isCollect_false() {
        val entity = MovieHistoryEntity(
            id = 7,
            title = "The Matrix",
            posterPath = "/poster.jpg",
            voteAverage = 8.7,
            releaseDate = "1999-03-31",
            timestamp = 2_000L,
        )

        val result = entity.asExtendedModel()

        assertEquals(
            MovieCardResult(
                id = 7,
                title = "The Matrix",
                posterPath = "/poster.jpg",
                voteAverage = 8.7,
                releaseDate = "1999-03-31",
                timestamp = 2_000L,
                isCollect = false,
            ),
            result,
        )
    }
}
