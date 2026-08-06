package com.shang.jetpackmoviekmp.feature.home.ui

import com.shang.jetpackmoviekmp.common.AppError
import com.shang.jetpackmoviekmp.common.AppResult
import com.shang.jetpackmoviekmp.common.UiState
import com.shang.jetpackmoviekmp.model.LanguageMode
import com.shang.jetpackmoviekmp.model.MovieGenreBean
import com.shang.jetpackmoviekmp.model.ThemeMode
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
            movieGenresResult = AppResult.Success(genres)
        }
        val viewModel = createViewModel(movieRepository = movieRepository)

        // Act
        val job = viewModel.movieGenres.launchIn(this)

        // Assert
        assertEquals(UiState.Success(genres), viewModel.movieGenres.value)
        job.cancel()
    }

    @Test
    fun `movieGenres 在 repository 回傳失敗時進入 Error`() = runTest(dispatcher) {
        // Arrange
        val movieRepository = FakeMovieRepository().apply {
            movieGenresResult = AppResult.Failure(AppError.Unknown)
        }
        val viewModel = createViewModel(movieRepository = movieRepository)

        // Act
        val job = viewModel.movieGenres.launchIn(this)

        // Assert
        assertIs<UiState.Error>(viewModel.movieGenres.value)
        job.cancel()
    }

    @Test
    fun `retry 會重新觸發 movieGenres 載入`() = runTest(dispatcher) {
        // Arrange：先讓第一次載入失敗
        val movieRepository = FakeMovieRepository().apply {
            movieGenresResult = AppResult.Failure(AppError.Unknown)
        }
        val viewModel = createViewModel(movieRepository = movieRepository)
        val job = viewModel.movieGenres.launchIn(this)
        assertIs<UiState.Error>(viewModel.movieGenres.value)

        val genres = MovieGenreBean(genres = listOf(MovieGenreBean.MovieGenre(id = 28, name = "Action")))
        movieRepository.movieGenresResult = AppResult.Success(genres)

        // Act
        viewModel.retry()

        // Assert
        assertEquals(UiState.Success(genres), viewModel.movieGenres.value)
        job.cancel()
    }

    @Test
    fun `languageMode 變化時重新呼叫 getMovieGenres`() = runTest(dispatcher) {
        // Arrange
        val movieRepository = FakeMovieRepository().apply {
            movieGenresResult = AppResult.Success(MovieGenreBean(genres = listOf(MovieGenreBean.MovieGenre(id = 1, name = "Old"))))
        }
        val userDataRepository = FakeUserDataRepository()
        val viewModel = createViewModel(movieRepository = movieRepository, userDataRepository = userDataRepository)
        val job = viewModel.movieGenres.launchIn(this)
        assertEquals(
            UiState.Success(MovieGenreBean(genres = listOf(MovieGenreBean.MovieGenre(id = 1, name = "Old")))),
            viewModel.movieGenres.value,
        )

        val newGenres = MovieGenreBean(genres = listOf(MovieGenreBean.MovieGenre(id = 2, name = "New")))
        movieRepository.movieGenresResult = AppResult.Success(newGenres)

        // Act
        userDataRepository.setLanguageMode(LanguageMode.ENGLISH)

        // Assert
        assertEquals(UiState.Success(newGenres), viewModel.movieGenres.value)
        job.cancel()
    }

    @Test
    fun `僅 themeMode 變化不觸發重新載入`() = runTest(dispatcher) {
        // Arrange
        val movieRepository = FakeMovieRepository().apply {
            movieGenresResult = AppResult.Success(MovieGenreBean(genres = listOf(MovieGenreBean.MovieGenre(id = 1, name = "Old"))))
        }
        val userDataRepository = FakeUserDataRepository()
        val viewModel = createViewModel(movieRepository = movieRepository, userDataRepository = userDataRepository)
        val job = viewModel.movieGenres.launchIn(this)
        val loadedGenres = viewModel.movieGenres.value

        movieRepository.movieGenresResult = AppResult.Success(MovieGenreBean(genres = listOf(MovieGenreBean.MovieGenre(id = 2, name = "New"))))

        // Act
        userDataRepository.setThemeMode(ThemeMode.DARK)

        // Assert
        assertEquals(loadedGenres, viewModel.movieGenres.value)
        job.cancel()
    }
}
