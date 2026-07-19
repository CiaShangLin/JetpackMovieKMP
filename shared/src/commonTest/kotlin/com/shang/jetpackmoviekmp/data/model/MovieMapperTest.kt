package com.shang.jetpackmoviekmp.data.model

import com.shang.jetpackmoviekmp.model.MovieCardResult
import kotlin.test.Test
import kotlin.test.assertEquals

class MovieMapperTest {

    private val movieCardResult = MovieCardResult(
        id = 42,
        title = "Test Movie",
        posterPath = "/poster.jpg",
        voteAverage = 7.5,
        releaseDate = "2024-01-01",
        isCollect = true,
        timestamp = 1_700_000_000_000L,
    )

    @Test
    fun asCollectEntity_maps_fields_from_movieCardResult() {
        val entity = movieCardResult.asCollectEntity()

        assertEquals(movieCardResult.id, entity.id)
        assertEquals(movieCardResult.title, entity.title)
        assertEquals(movieCardResult.posterPath, entity.posterPath)
        assertEquals(movieCardResult.voteAverage, entity.voteAverage)
        assertEquals(movieCardResult.releaseDate, entity.releaseDate)
        assertEquals(movieCardResult.timestamp, entity.timestamp)
    }

    @Test
    fun asHistoryEntity_maps_fields_from_movieCardResult() {
        val entity = movieCardResult.asHistoryEntity()

        assertEquals(movieCardResult.id, entity.id)
        assertEquals(movieCardResult.title, entity.title)
        assertEquals(movieCardResult.posterPath, entity.posterPath)
        assertEquals(movieCardResult.voteAverage, entity.voteAverage)
        assertEquals(movieCardResult.releaseDate, entity.releaseDate)
        assertEquals(movieCardResult.timestamp, entity.timestamp)
    }
}
