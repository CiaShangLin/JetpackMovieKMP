package com.shang.jetpackmoviekmp.feature.history.ui

import com.shang.jetpackmoviekmp.domain.usecase.GetHistoryMovieListUseCase
import com.shang.jetpackmoviekmp.model.MovieCardResult
import com.shang.jetpackmoviekmp.model.asMovieCardData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `history movies with collected id become Success with collected status`() = runTest(dispatcher) {
        // Arrange
        val repository = FakeMovieRepository().apply {
            historyMovies.value = listOf(MovieCardResult(id = 7, title = "歷史電影"))
            collectedMovieIds.value = listOf(7)
        }
        val viewModel = HistoryViewModel(historyUseCase(repository), repository)

        // Act
        val state = viewModel.historyMovies.filterIsInstance<HistoryUiState.Success>().first()

        // Assert
        assertEquals(
            listOf(MovieCardResult(id = 7, title = "歷史電影", isCollect = true)),
            state.historyList,
            "歷史電影應轉為帶有收藏狀態的 Success state",
        )
    }

    @Test
    fun `empty history becomes Empty state`() = runTest(dispatcher) {
        // Arrange
        val repository = FakeMovieRepository()
        val viewModel = HistoryViewModel(historyUseCase(repository), repository)

        // Act
        val state = viewModel.historyMovies.first()

        // Assert
        assertEquals(HistoryUiState.Empty, state, "空歷史清單應維持 Empty state")
    }

    @Test
    fun `history clearing emission changes Success state to Empty`() = runTest(dispatcher) {
        // Arrange
        val repository = FakeMovieRepository().apply {
            historyMovies.value = listOf(MovieCardResult(id = 10, title = "待清空電影"))
        }
        val viewModel = HistoryViewModel(historyUseCase(repository), repository)
        val successState = async {
            viewModel.historyMovies.filterIsInstance<HistoryUiState.Success>().first()
        }
        runCurrent()
        successState.await()
        val emptyState = async {
            viewModel.historyMovies.filterIsInstance<HistoryUiState.Empty>().first()
        }

        // Act
        repository.historyMovies.value = emptyList()
        runCurrent()

        // Assert
        assertEquals(HistoryUiState.Empty, emptyState.await(), "清空後應切換為 Empty state")
    }

    @Test
    fun `toggling an already collected movie removes the same movie`() = runTest(dispatcher) {
        // Arrange
        val repository = FakeMovieRepository()
        val viewModel = HistoryViewModel(historyUseCase(repository), repository)
        val movie = MovieCardResult(id = 8, title = "已收藏", isCollect = true)

        // Act
        viewModel.toggleMovieCollect(movie.asMovieCardData())

        // Assert
        assertTrue(repository.deleteMovieCollectLatch.await(1, TimeUnit.SECONDS), "應完成取消收藏操作")
        assertEquals(movie, repository.deletedMovie, "取消收藏的電影應與點擊項目相同")
    }

    @Test
    fun `toggling an uncollected movie inserts the same movie`() = runTest(dispatcher) {
        // Arrange
        val repository = FakeMovieRepository()
        val viewModel = HistoryViewModel(historyUseCase(repository), repository)
        val movie = MovieCardResult(id = 9, title = "未收藏")

        // Act
        viewModel.toggleMovieCollect(movie.asMovieCardData())

        // Assert
        assertTrue(repository.insertMovieCollectLatch.await(1, TimeUnit.SECONDS), "應完成加入收藏操作")
        assertEquals(movie, repository.insertedMovie, "加入收藏的電影應與點擊項目相同")
    }

    @Test
    fun `clearing history calls repository once`() = runTest(dispatcher) {
        // Arrange
        val repository = FakeMovieRepository()
        val viewModel = HistoryViewModel(historyUseCase(repository), repository)

        // Act
        viewModel.clearHistory()

        // Assert
        assertTrue(repository.clearHistoryLatch.await(1, TimeUnit.SECONDS), "應完成清空歷史操作")
        assertEquals(1, repository.clearHistoryCallCount, "清空歷史應只呼叫一次")
    }

    private fun historyUseCase(repository: FakeMovieRepository) = GetHistoryMovieListUseCase(repository, dispatcher)
}
