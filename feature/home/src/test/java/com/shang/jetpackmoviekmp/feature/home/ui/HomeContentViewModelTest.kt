package com.shang.jetpackmoviekmp.feature.home.ui

import androidx.paging.PagingData
import com.shang.jetpackmoviekmp.core.ui.asMovieCardData
import com.shang.jetpackmoviekmp.domain.usecase.GetHomeMovieListUseCase
import com.shang.jetpackmoviekmp.model.MovieCardResult
import com.shang.jetpackmoviekmp.model.MovieGenreBean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HomeContentViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val genre = MovieGenreBean.MovieGenre(id = 28, name = "Action")

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(movieRepository: FakeMovieRepository): HomeContentViewModel {
        val useCase = GetHomeMovieListUseCase(
            movieRepository = movieRepository,
            ioDispatcher = dispatcher,
        )
        return HomeContentViewModel(
            movieRepository = movieRepository,
            getMovieGenreUseCase = useCase,
            movieGenre = genre,
        )
    }

    // 分頁邏輯本身比照 GetHomeMovieListUseCaseTest 做 collectible smoke test，不斷言
    // PagingData 實際內容——專案未引入 androidx.paging:paging-testing，見 design.md 風險章節。
    @Test
    fun `movieList 可被收集而不拋出例外`() = runTest(dispatcher) {
        // Arrange
        val movieRepository = FakeMovieRepository().apply {
            movieListPager = flowOf(PagingData.from(listOf(MovieCardResult(id = 1, title = "A"))))
        }
        val viewModel = createViewModel(movieRepository)

        // Act
        val pagingData = viewModel.movieList.first()

        // Assert
        assertNotNull(pagingData)
    }

    @Test
    fun `toggleMovieCollectStatus 對尚未收藏的電影會呼叫 insertMovieCollect`() = runTest(dispatcher) {
        // Arrange
        val movieRepository = FakeMovieRepository()
        val viewModel = createViewModel(movieRepository)
        val data = MovieCardResult(id = 1, title = "A", isCollect = false).asMovieCardData()

        // Act
        viewModel.toggleMovieCollectStatus(data)

        // Assert：insertMovieCollect 在 viewModelScope.launch(Dispatchers.IO) 的真實執行緒上
        // 執行，用 latch 確定性等待，避免用 Thread.sleep 猜測時間
        assertTrue(movieRepository.insertMovieCollectLatch.await(1, TimeUnit.SECONDS))
        assertEquals(1, movieRepository.insertMovieCollectCallCount)
        assertEquals(0, movieRepository.deleteMovieCollectCallCount)
    }

    @Test
    fun `toggleMovieCollectStatus 對已收藏的電影會呼叫 deleteMovieCollect`() = runTest(dispatcher) {
        // Arrange
        val movieRepository = FakeMovieRepository()
        val viewModel = createViewModel(movieRepository)
        val data = MovieCardResult(id = 1, title = "A", isCollect = true).asMovieCardData()

        // Act
        viewModel.toggleMovieCollectStatus(data)

        // Assert
        assertTrue(movieRepository.deleteMovieCollectLatch.await(1, TimeUnit.SECONDS))
        assertEquals(1, movieRepository.deleteMovieCollectCallCount)
        assertEquals(0, movieRepository.insertMovieCollectCallCount)
    }
}
