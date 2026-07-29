package com.shang.jetpackmoviekmp.domain.usecase

import androidx.paging.PagingData
import com.shang.jetpackmoviekmp.domain.FakeMovieRepository
import com.shang.jetpackmoviekmp.model.MovieCardResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class GetSearchMovieListUseCaseTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun invoke_取得初始搜尋結果() = runTest {
        // Arrange
        val movieRepository = FakeMovieRepository().apply {
            movieSearchPager = flowOf(PagingData.from(listOf(MovieCardResult(id = 1, title = "Dune"))))
            collectedMovieIdsFlow.value = listOf(1)
        }
        val useCase = GetSearchMovieListUseCase(
            movieRepository = movieRepository,
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )

        // Act
        val pagingData = useCase(query = "Dune").first()

        // Assert
        assertNotNull(pagingData)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun invoke_收藏資料變更後重新發出搜尋結果() = runTest {
        // Arrange
        val movieRepository = FakeMovieRepository().apply {
            movieSearchPager = flowOf(PagingData.from(listOf(MovieCardResult(id = 1, title = "Dune"))))
        }
        val useCase = GetSearchMovieListUseCase(
            movieRepository = movieRepository,
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )

        // Act
        val pagingDataEmissions = mutableListOf<PagingData<MovieCardResult>>()
        val collectJob = launch {
            useCase(query = "Dune")
                .take(2)
                .toList(pagingDataEmissions)
        }
        runCurrent()
        movieRepository.collectedMovieIdsFlow.value = listOf(1)
        runCurrent()
        collectJob.join()

        // Assert
        assertEquals(2, pagingDataEmissions.size, "收藏資料更新時應重新發出搜尋結果")
    }
}
