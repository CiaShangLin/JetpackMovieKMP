package com.shang.jetpackmoviekmp.feature.collect.ui

import com.shang.jetpackmoviekmp.model.MovieCardResult
import com.shang.jetpackmoviekmp.model.asMovieCardData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CollectViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `allMovieCollect 將收藏電影轉為 Success state`() = runTest(dispatcher) {
        // Arrange
        val movie = MovieCardResult(id = 1, title = "收藏電影", isCollect = true)
        val repository = FakeMovieRepository().apply {
            collectedMovies.value = listOf(movie)
        }
        val viewModel = CollectViewModel(repository)

        // Act
        val state = viewModel.allMovieCollect.filterIsInstance<CollectUiState.Success>().first()

        // Assert
        assertEquals(listOf(movie), state.movieCollectList, "收藏清單應轉為 Success state")
    }

    @Test
    fun `allMovieCollect 保留 repository 提供的最新收藏順序`() = runTest(dispatcher) {
        // Arrange
        val newestMovie = MovieCardResult(id = 2, title = "最新收藏", isCollect = true)
        val oldestMovie = MovieCardResult(id = 1, title = "較早收藏", isCollect = true)
        val repository = FakeMovieRepository().apply {
            collectedMovies.value = listOf(newestMovie, oldestMovie)
        }
        val viewModel = CollectViewModel(repository)

        // Act
        val state = viewModel.allMovieCollect.filterIsInstance<CollectUiState.Success>().first()

        // Assert
        assertEquals(listOf(newestMovie, oldestMovie), state.movieCollectList)
    }

    @Test
    fun `allMovieCollect 在 repository 發出空清單時轉為 Success empty state`() = runTest(dispatcher) {
        // Arrange
        val viewModel = CollectViewModel(FakeMovieRepository())

        // Act
        val state = viewModel.allMovieCollect.filterIsInstance<CollectUiState.Success>().first()

        // Assert
        assertEquals(emptyList(), state.movieCollectList, "空收藏清單應轉為 Success empty state")
    }

    @Test
    fun `removeMovieCollect 對已收藏電影會呼叫 deleteMovieCollect`() = runTest(dispatcher) {
        // Arrange
        val repository = FakeMovieRepository()
        val viewModel = CollectViewModel(repository)
        val movie = MovieCardResult(id = 1, title = "收藏電影", isCollect = true)

        // Act
        viewModel.removeMovieCollect(movie.asMovieCardData())

        // Assert
        assertTrue(
            repository.deleteMovieCollectLatch.await(1, TimeUnit.SECONDS),
            "已收藏電影應觸發刪除操作",
        )
        assertEquals(1, repository.deleteMovieCollectCallCount, "刪除操作應只呼叫一次")
        assertEquals(movie.id, repository.deletedMovie?.id, "刪除的電影 id 應與點擊項目相同")
    }

    @Test
    fun `取消收藏清單中一部電影後，其餘電影 id 仍對應各自原本的電影`() = runTest(dispatcher) {
        // Arrange：收藏清單同時含多部電影
        val remainingMovieA = MovieCardResult(id = 1, title = "保留電影 A", isCollect = true)
        val targetMovie = MovieCardResult(id = 2, title = "待取消電影", isCollect = true)
        val remainingMovieB = MovieCardResult(id = 3, title = "保留電影 B", isCollect = true)
        val repository = FakeMovieRepository().apply {
            collectedMovies.value = listOf(remainingMovieA, targetMovie, remainingMovieB)
        }
        val viewModel = CollectViewModel(repository)

        // Act：只對清單中間那部電影觸發取消收藏
        viewModel.removeMovieCollect(targetMovie.asMovieCardData())

        // Assert：只刪除目標電影，其餘電影的 id 與資料未被觸及
        assertTrue(repository.deleteMovieCollectLatch.await(1, TimeUnit.SECONDS))
        assertEquals(1, repository.deleteMovieCollectCallCount, "刪除操作應只呼叫一次")
        assertEquals(targetMovie.id, repository.deletedMovie?.id)
        assertNotEquals(remainingMovieA.id, repository.deletedMovie?.id)
        assertNotEquals(remainingMovieB.id, repository.deletedMovie?.id)
    }
}
