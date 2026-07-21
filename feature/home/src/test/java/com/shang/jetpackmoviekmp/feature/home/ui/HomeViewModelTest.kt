package com.shang.jetpackmoviekmp.feature.home.ui

import com.shang.jetpackmoviekmp.model.MovieGenreBean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        movieRepository: FakeMovieRepository = FakeMovieRepository(),
        userDataRepository: FakeUserDataRepository = FakeUserDataRepository(),
    ) = HomeViewModel(
        userDataRepository = userDataRepository,
        movieRepository = movieRepository,
    )

    @Test
    fun `movieGenres 在 repository 回傳成功時進入 Success`() = runTest(dispatcher) {
        // Arrange
        val genres = MovieGenreBean(genres = listOf(MovieGenreBean.MovieGenre(id = 28, name = "Action")))
        val movieRepository = FakeMovieRepository().apply {
            movieGenresResult = Result.success(genres)
        }
        val viewModel = createViewModel(movieRepository = movieRepository)

        // Act
        val job = viewModel.movieGenres.launchIn(this)

        // Assert
        assertEquals(HomeUiState.Success(genres), viewModel.movieGenres.value)
        job.cancel()
    }

    @Test
    fun `movieGenres 在 repository 回傳失敗時進入 Error`() = runTest(dispatcher) {
        // Arrange
        val movieRepository = FakeMovieRepository().apply {
            movieGenresResult = Result.failure(IllegalStateException("boom"))
        }
        val viewModel = createViewModel(movieRepository = movieRepository)

        // Act
        val job = viewModel.movieGenres.launchIn(this)

        // Assert
        assertIs<HomeUiState.Error>(viewModel.movieGenres.value)
        job.cancel()
    }

    @Test
    fun `retry 會重新觸發 movieGenres 載入`() = runTest(dispatcher) {
        // Arrange：先讓第一次載入失敗
        val movieRepository = FakeMovieRepository().apply {
            movieGenresResult = Result.failure(IllegalStateException("boom"))
        }
        val viewModel = createViewModel(movieRepository = movieRepository)
        val job = viewModel.movieGenres.launchIn(this)
        assertIs<HomeUiState.Error>(viewModel.movieGenres.value)

        val genres = MovieGenreBean(genres = listOf(MovieGenreBean.MovieGenre(id = 28, name = "Action")))
        movieRepository.movieGenresResult = Result.success(genres)

        // Act
        viewModel.retry()

        // Assert
        assertEquals(HomeUiState.Success(genres), viewModel.movieGenres.value)
        job.cancel()
    }
}
