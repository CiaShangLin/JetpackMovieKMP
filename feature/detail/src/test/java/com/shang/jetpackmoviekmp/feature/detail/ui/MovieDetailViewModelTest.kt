package com.shang.jetpackmoviekmp.feature.detail.ui

import androidx.paging.PagingData
import com.shang.jetpackmoviekmp.common.AppResult
import com.shang.jetpackmoviekmp.common.UiState
import com.shang.jetpackmoviekmp.data.repository.MovieRepository
import com.shang.jetpackmoviekmp.data.repository.UserDataRepository
import com.shang.jetpackmoviekmp.domain.usecase.GetMovieDetailUseCase
import com.shang.jetpackmoviekmp.domain.usecase.GetMovieRecommendUseCase
import com.shang.jetpackmoviekmp.model.ConfigurationBean
import com.shang.jetpackmoviekmp.model.LanguageMode
import com.shang.jetpackmoviekmp.model.MovieCardData
import com.shang.jetpackmoviekmp.model.MovieCardResult
import com.shang.jetpackmoviekmp.model.MovieCastAndCrewBean
import com.shang.jetpackmoviekmp.model.MovieDetailBean
import com.shang.jetpackmoviekmp.model.MovieGenreBean
import com.shang.jetpackmoviekmp.model.MovieRecommendBean
import com.shang.jetpackmoviekmp.model.ThemeMode
import com.shang.jetpackmoviekmp.model.UserData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class MovieDetailViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `detail success exposes movie and writes history`() = runTest(dispatcher) {
        // Arrange
        val repository = FakeMovieRepository().apply {
            detailResult = Result.success(MovieDetailBean(id = MOVIE_ID, title = "Dune"))
        }
        val viewModel = createViewModel(repository)

        // Act
        val job = viewModel.movieDetail.launchIn(this)
        runCurrent()

        // Assert
        assertEquals(UiState.Success(MovieDetailBean(id = MOVIE_ID, title = "Dune")), viewModel.movieDetail.value)
        assertEquals(listOf(MOVIE_ID), repository.historyIds)
        job.cancel()
    }

    @Test
    fun `detail retry requests the same movie again`() = runTest(dispatcher) {
        // Arrange
        val repository = FakeMovieRepository().apply {
            detailResult = Result.failure(IllegalStateException("network"))
        }
        val viewModel = createViewModel(repository)
        val job = viewModel.movieDetail.launchIn(this)
        runCurrent()

        // Act
        viewModel.retryMovieDetail()
        runCurrent()

        // Assert
        assertEquals(2, repository.detailRequests)
        assertIs<UiState.Error>(viewModel.movieDetail.value)
        job.cancel()
    }

    @Test
    fun `collect toggle inserts or deletes according to current card state`() = runTest(dispatcher) {
        // Arrange
        val repository = FakeMovieRepository()
        val viewModel = createViewModel(repository)

        // Act
        viewModel.toggleCollect(cardData(isCollect = false))
        viewModel.toggleCollect(cardData(isCollect = true))
        runCurrent()

        // Assert
        assertEquals(listOf(MOVIE_ID), repository.insertedCollectIds)
        assertEquals(listOf(MOVIE_ID), repository.deletedCollectIds)
    }

    @Test
    fun `failed actors and recommendations expose independent error states`() = runTest(dispatcher) {
        // Arrange
        val repository = FakeMovieRepository().apply {
            actorResult = AppResult.Failure(com.shang.jetpackmoviekmp.common.AppError.Unknown)
            recommendationResult = Result.failure(IllegalStateException("recommendations"))
        }
        val viewModel = createViewModel(repository)

        // Act
        val actorJob = viewModel.movieActors.launchIn(this)
        val recommendationJob = viewModel.movieRecommendations.launchIn(this)
        runCurrent()

        // Assert
        assertIs<UiState.Error>(viewModel.movieActors.value)
        assertIs<UiState.Error>(viewModel.movieRecommendations.value)
        actorJob.cancel()
        recommendationJob.cancel()
    }

    @Test
    fun `languageMode 變化時 movieDetail、movieRecommendations、movieActors 分別重新呼叫對應 UseCase`() = runTest(dispatcher) {
        // Arrange
        val repository = FakeMovieRepository()
        val userDataRepository = FakeUserDataRepository()
        val viewModel = createViewModel(repository, userDataRepository)
        val detailJob = viewModel.movieDetail.launchIn(this)
        val actorJob = viewModel.movieActors.launchIn(this)
        val recommendationJob = viewModel.movieRecommendations.launchIn(this)
        runCurrent()
        assertEquals(1, repository.detailRequests)
        assertEquals(1, repository.actorRequests)
        assertEquals(1, repository.recommendationRequests)

        // Act
        userDataRepository.setLanguageMode(LanguageMode.ENGLISH)
        runCurrent()

        // Assert
        assertEquals(2, repository.detailRequests)
        assertEquals(2, repository.actorRequests)
        assertEquals(2, repository.recommendationRequests)
        detailJob.cancel()
        actorJob.cancel()
        recommendationJob.cancel()
    }

    @Test
    fun `retryMovieDetail 只重試 movieDetail，不影響 movieActors 與 movieRecommendations`() = runTest(dispatcher) {
        // Arrange
        val repository = FakeMovieRepository()
        val viewModel = createViewModel(repository)
        val detailJob = viewModel.movieDetail.launchIn(this)
        val actorJob = viewModel.movieActors.launchIn(this)
        val recommendationJob = viewModel.movieRecommendations.launchIn(this)
        runCurrent()
        assertEquals(1, repository.detailRequests)
        assertEquals(1, repository.actorRequests)
        assertEquals(1, repository.recommendationRequests)

        // Act
        viewModel.retryMovieDetail()
        runCurrent()

        // Assert
        assertEquals(2, repository.detailRequests)
        assertEquals(1, repository.actorRequests)
        assertEquals(1, repository.recommendationRequests)
        detailJob.cancel()
        actorJob.cancel()
        recommendationJob.cancel()
    }

    private fun createViewModel(
        repository: MovieRepository,
        userDataRepository: UserDataRepository = FakeUserDataRepository(),
    ) = MovieDetailViewModel(
        movieRepository = repository,
        userDataRepository = userDataRepository,
        getMovieDetailUseCase = GetMovieDetailUseCase(repository, dispatcher),
        getMovieRecommendUseCase = GetMovieRecommendUseCase(repository, dispatcher),
        movieId = MOVIE_ID,
    )

    private fun cardData(isCollect: Boolean) = MovieCardData(
        movieCardId = MOVIE_ID,
        movieCardTitle = "Dune",
        movieCardPosterPath = "/poster.jpg",
        movieCardReleaseDate = "2021-10-22",
        movieCardVoteAverage = 8.0,
        movieCardIsCollect = isCollect,
        movieCardTimestamp = 0L,
    )

    private class FakeMovieRepository : MovieRepository {
        var detailResult: Result<MovieDetailBean> = Result.success(MovieDetailBean(id = MOVIE_ID))
        var actorResult: AppResult<MovieCastAndCrewBean> = AppResult.Success(MovieCastAndCrewBean())
        var recommendationResult: Result<MovieRecommendBean> = Result.success(MovieRecommendBean())
        var detailRequests = 0
        var recommendationRequests = 0
        var actorRequests = 0
        val insertedCollectIds = mutableListOf<Int>()
        val deletedCollectIds = mutableListOf<Int>()
        val historyIds = mutableListOf<Int>()
        private val collectedIds = MutableStateFlow(emptyList<Int>())

        override fun getConfiguration(): Flow<Result<ConfigurationBean>> = flowOf(Result.success(ConfigurationBean()))
        override fun getMovieGenres(): Flow<AppResult<MovieGenreBean>> = flowOf(AppResult.Success(MovieGenreBean()))
        override fun getMovieListPager(withGenres: String): Flow<PagingData<MovieCardResult>> = flowOf(PagingData.empty())
        override fun getMovieSearchPager(query: String): Flow<PagingData<MovieCardResult>> = flowOf(PagingData.empty())
        override fun getMovieDetail(id: Int): Flow<Result<MovieDetailBean>> {
            detailRequests++
            return flowOf(detailResult)
        }
        override fun getMovieRecommendations(id: Int): Flow<Result<MovieRecommendBean>> {
            recommendationRequests++
            return flowOf(recommendationResult)
        }
        override fun getMovieActor(id: Int): Flow<AppResult<MovieCastAndCrewBean>> {
            actorRequests++
            return flowOf(actorResult)
        }
        override suspend fun insertMovieCollect(movieResult: MovieCardResult) { insertedCollectIds += movieResult.id }
        override suspend fun deleteMovieCollect(movieResult: MovieCardResult) { deletedCollectIds += movieResult.id }
        override fun getCollectedMovieIds(): Flow<List<Int>> = collectedIds
        override fun getAllMovieCollect(): Flow<List<MovieCardResult>> = flowOf(emptyList())
        override fun getMovieCollectEntityById(id: Int): Flow<MovieCardResult?> = flowOf(null)
        override suspend fun insertMovieHistory(movieResult: MovieCardResult) { historyIds += movieResult.id }
        override suspend fun deleteMovieHistory(movieResult: MovieCardResult) = Unit
        override fun getAllMovieHistory(): Flow<List<MovieCardResult>> = flowOf(emptyList())
        override suspend fun deleteAllMovieHistory(): Boolean = true
    }

    private class FakeUserDataRepository(
        initial: UserData = UserData.getDefault(),
    ) : UserDataRepository {

        private val state = MutableStateFlow(initial)

        override val userData: Flow<UserData> get() = state

        override suspend fun setConfiguration(configuration: ConfigurationBean) {
            state.value = state.value.copy(configuration = configuration)
        }

        override suspend fun setThemeMode(themeMode: ThemeMode) {
            state.value = state.value.copy(themeMode = themeMode)
        }

        override suspend fun setLanguageMode(languageMode: LanguageMode) {
            state.value = state.value.copy(languageMode = languageMode)
        }
    }

    private companion object {
        const val MOVIE_ID = 1
    }
}
