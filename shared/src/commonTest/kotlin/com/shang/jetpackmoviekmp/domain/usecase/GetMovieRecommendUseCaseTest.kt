package com.shang.jetpackmoviekmp.domain.usecase

import com.shang.jetpackmoviekmp.domain.FakeMovieRepository
import com.shang.jetpackmoviekmp.model.MovieCardResult
import com.shang.jetpackmoviekmp.model.MovieRecommendBean
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class GetMovieRecommendUseCaseTest {

    // 見 GetHistoryMovieListUseCaseTest 的說明：`combine()` 需要 ioDispatcher 與
    // runTest 共用同一個 TestCoroutineScheduler，否則會拋出
    // 「Detected use of different schedulers」。
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun TestScope.useCase(movieRepository: FakeMovieRepository) = GetMovieRecommendUseCase(
        movieRepository = movieRepository,
        ioDispatcher = UnconfinedTestDispatcher(testScheduler),
    )

    @Test
    fun invoke_marks_isCollect_when_recommendations_call_succeeds() = runTest {
        val recommendations = MovieRecommendBean(results = listOf(MovieCardResult(id = 1, title = "A")))
        val movieRepository = FakeMovieRepository().apply {
            movieRecommendationsResult = Result.success(recommendations)
            collectedMovieIdsFlow.value = listOf(1)
        }

        val result = useCase(movieRepository).invoke(movieId = 7).first()

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.first { it.id == 1 }?.isCollect == true)
    }

    @Test
    fun invoke_emits_original_failure_when_recommendations_call_fails() = runTest {
        val movieRepository = FakeMovieRepository().apply {
            movieRecommendationsResult = Result.failure(IllegalStateException("boom"))
        }

        val result = useCase(movieRepository).invoke(movieId = 7).first()

        assertTrue(result.isFailure)
    }
}
