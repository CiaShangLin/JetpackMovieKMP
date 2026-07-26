package com.shang.jetpackmoviekmp.model

import kotlin.test.Test
import kotlin.test.assertEquals

class MovieCardDataTest {

    @Test
    fun asMovieCardData_maps_all_fields_from_movieCardResult() {
        // Arrange
        val result = MovieCardResult(
            id = 42,
            title = "Sample Movie",
            posterPath = "/poster.jpg",
            releaseDate = "2024-01-01",
            voteAverage = 8.5,
            isCollect = true,
            timestamp = 1_700_000_000L,
        )

        // Act
        val data = result.asMovieCardData()

        // Assert
        assertEquals(result.id, data.movieCardId)
        assertEquals(result.title, data.movieCardTitle)
        assertEquals(result.posterPath, data.movieCardPosterPath)
        assertEquals(result.releaseDate, data.movieCardReleaseDate)
        assertEquals(result.voteAverage, data.movieCardVoteAverage)
        assertEquals(result.isCollect, data.movieCardIsCollect)
        assertEquals(result.timestamp, data.movieCardTimestamp)
    }

    @Test
    fun asMovieCardResult_maps_all_fields_from_movieCardData() {
        // Arrange
        val data = MovieCardData(
            movieCardId = 7,
            movieCardTitle = "Another Movie",
            movieCardPosterPath = "/another.jpg",
            movieCardReleaseDate = "2023-05-20",
            movieCardVoteAverage = 6.2,
            movieCardIsCollect = false,
            movieCardTimestamp = 1_600_000_000L,
        )

        // Act
        val result = data.asMovieCardResult()

        // Assert
        assertEquals(data.movieCardId, result.id)
        assertEquals(data.movieCardTitle, result.title)
        assertEquals(data.movieCardPosterPath, result.posterPath)
        assertEquals(data.movieCardReleaseDate, result.releaseDate)
        assertEquals(data.movieCardVoteAverage, result.voteAverage)
        assertEquals(data.movieCardIsCollect, result.isCollect)
        assertEquals(data.movieCardTimestamp, result.timestamp)
    }

    @Test
    fun roundTrip_conversion_preserves_movieCardResult_values() {
        // Arrange
        val original = MovieCardResult(
            id = 99,
            title = "Round Trip Movie",
            posterPath = "/round-trip.jpg",
            releaseDate = "2022-12-25",
            voteAverage = 9.1,
            isCollect = true,
            timestamp = 1_650_000_000L,
        )

        // Act
        val roundTripped = original.asMovieCardData().asMovieCardResult()

        // Assert
        assertEquals(original, roundTripped)
    }
}
