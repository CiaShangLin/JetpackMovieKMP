package com.shang.jetpackmoviekmp.domain.usecase

import com.shang.jetpackmoviekmp.domain.FakeMovieRepository
import com.shang.jetpackmoviekmp.model.MovieDetailBean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GetMovieDetailUseCaseTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun useCase(movieRepository: FakeMovieRepository) = GetMovieDetailUseCase(
        movieRepository = movieRepository,
        ioDispatcher = UnconfinedTestDispatcher(),
    )

    @Test
    fun invoke_inserts_movie_history_once_when_detail_call_succeeds() = runTest {
        val detail = MovieDetailBean(id = 7, title = "Detail")
        val movieRepository = FakeMovieRepository().apply {
            movieDetailResult = Result.success(detail)
        }

        val result = useCase(movieRepository).invoke(7).first()

        assertEquals(detail, result.getOrNull())
        assertEquals(1, movieRepository.insertMovieHistoryCallCount)
        assertEquals(detail.id, movieRepository.lastInsertedMovieHistory?.id)
    }

    @Test
    fun invoke_does_not_insert_movie_history_when_detail_call_fails() = runTest {
        val movieRepository = FakeMovieRepository().apply {
            movieDetailResult = Result.failure(IllegalStateException("boom"))
        }

        val result = useCase(movieRepository).invoke(7).first()

        assertTrue(result.isFailure)
        assertEquals(0, movieRepository.insertMovieHistoryCallCount)
    }

    @Test
    fun invoke_still_emits_detail_when_movie_history_write_fails() = runTest {
        val detail = MovieDetailBean(id = 7, title = "Detail")
        val movieRepository = FakeMovieRepository().apply {
            movieDetailResult = Result.success(detail)
            insertMovieHistoryThrows = IllegalStateException("db write failed")
        }

        val result = useCase(movieRepository).invoke(7).first()

        assertEquals(detail, result.getOrNull())
    }

    @Test
    fun invoke_rethrows_cancellationException_from_movie_history_write_instead_of_swallowing_it() = runTest {
        val movieRepository = FakeMovieRepository().apply {
            movieDetailResult = Result.success(MovieDetailBean(id = 7, title = "Detail"))
            insertMovieHistoryThrows = CancellationException("cancelled")
        }

        assertFailsWith<CancellationException> {
            useCase(movieRepository).invoke(7).first()
        }
    }
}
