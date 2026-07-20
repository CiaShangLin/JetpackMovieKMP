package com.shang.jetpackmoviekmp.domain.usecase

import com.shang.jetpackmoviekmp.domain.FakeMovieRepository
import com.shang.jetpackmoviekmp.model.MovieCardResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GetHistoryMovieListUseCaseTest {

    // `combine()` 內部用 channel/`produce` 實作，若 ioDispatcher 用「與 runTest 不同的
    // TestCoroutineScheduler」的 UnconfinedTestDispatcher 會觸發
    // 「Detected use of different schedulers」IllegalStateException，因此改用
    // TestScope 本身的 testScheduler，確保與 runTest 共用同一個 scheduler。
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun TestScope.useCase(movieRepository: FakeMovieRepository) = GetHistoryMovieListUseCase(
        movieRepository = movieRepository,
        ioDispatcher = UnconfinedTestDispatcher(testScheduler),
    )

    @Test
    fun invoke_marks_isCollect_true_for_movie_in_collected_ids() = runTest {
        val movie = MovieCardResult(id = 1, title = "A")
        val movieRepository = FakeMovieRepository().apply {
            movieHistoryFlow.value = listOf(movie)
            collectedMovieIdsFlow.value = listOf(1)
        }

        val result = useCase(movieRepository).invoke().first()

        assertTrue(result.first { it.id == 1 }.isCollect)
    }

    @Test
    fun invoke_marks_isCollect_false_for_movie_not_in_collected_ids() = runTest {
        val movie = MovieCardResult(id = 2, title = "B")
        val movieRepository = FakeMovieRepository().apply {
            movieHistoryFlow.value = listOf(movie)
            collectedMovieIdsFlow.value = emptyList()
        }

        val result = useCase(movieRepository).invoke().first()

        assertFalse(result.first { it.id == 2 }.isCollect)
    }
}
