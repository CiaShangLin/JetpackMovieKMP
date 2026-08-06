package com.shang.jetpackmoviekmp.feature.search.ui

import androidx.paging.PagingData
import com.shang.jetpackmoviekmp.common.AppResult
import com.shang.jetpackmoviekmp.data.repository.MovieRepository
import com.shang.jetpackmoviekmp.data.repository.UserDataRepository
import com.shang.jetpackmoviekmp.domain.usecase.GetSearchMovieListUseCase
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
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

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
    fun `初始空白 query 不搜尋`() = runTest(dispatcher) {
        // Arrange
        val repository = FakeMovieRepository()
        val viewModel = createViewModel(repository)
        val job = viewModel.movieSearchPager.launchIn(this)

        // Act
        runCurrent()

        // Assert
        assertEquals("", viewModel.searchQuery.value)
        assertTrue(repository.searchQueries.isEmpty())
        job.cancel()
    }

    @Test
    fun `提交非空 query 後搜尋電影`() = runTest(dispatcher) {
        // Arrange
        val repository = FakeMovieRepository()
        val viewModel = createViewModel(repository)
        val job = viewModel.movieSearchPager.launchIn(this)

        // Act
        viewModel.startSearch("Dune")
        advanceTimeBy(300)
        runCurrent()

        // Assert
        assertEquals("Dune", viewModel.searchQuery.value)
        assertEquals(listOf("Dune"), repository.searchQueries)
        job.cancel()
    }

    @Test
    fun `提交新 query 時改為搜尋新關鍵字`() = runTest(dispatcher) {
        // Arrange
        val repository = FakeMovieRepository()
        val viewModel = createViewModel(repository)
        val job = viewModel.movieSearchPager.launchIn(this)
        viewModel.startSearch("Dune")
        advanceTimeBy(300)
        runCurrent()

        // Act
        viewModel.startSearch("Avatar")
        advanceTimeBy(300)
        runCurrent()

        // Assert
        assertEquals(listOf("Dune", "Avatar"), repository.searchQueries)
        job.cancel()
    }

    @Test
    fun `retry 重新搜尋目前 query`() = runTest(dispatcher) {
        // Arrange
        val repository = FakeMovieRepository()
        val viewModel = createViewModel(repository)
        val job = viewModel.movieSearchPager.launchIn(this)
        viewModel.startSearch("Dune")
        advanceTimeBy(300)
        runCurrent()

        // Act
        viewModel.retrySearch()
        runCurrent()

        // Assert
        assertEquals(listOf("Dune", "Dune"), repository.searchQueries)
        job.cancel()
    }

    @Test
    fun `收藏未收藏電影時新增收藏資料`() = runTest(dispatcher) {
        // Arrange
        val repository = FakeMovieRepository()
        val viewModel = createViewModel(repository)
        val movie = MovieCardData(
            movieCardId = 1,
            movieCardTitle = "Dune",
            movieCardPosterPath = "",
            movieCardReleaseDate = "2021-10-22",
            movieCardVoteAverage = 8.0,
            movieCardIsCollect = false,
            movieCardTimestamp = 0L,
        )

        // Act
        viewModel.toggleMovieCollectStatus(movie)
        runCurrent()

        // Assert
        assertEquals(listOf(1), repository.insertedMovieCollectIds)
        assertTrue(repository.deletedMovieCollectIds.isEmpty())
    }

    @Test
    fun `取消收藏已收藏電影時刪除收藏資料`() = runTest(dispatcher) {
        // Arrange
        val repository = FakeMovieRepository()
        val viewModel = createViewModel(repository)
        val movie = MovieCardData(
            movieCardId = 1,
            movieCardTitle = "Dune",
            movieCardPosterPath = "",
            movieCardReleaseDate = "2021-10-22",
            movieCardVoteAverage = 8.0,
            movieCardIsCollect = true,
            movieCardTimestamp = 0L,
        )

        // Act
        viewModel.toggleMovieCollectStatus(movie)
        runCurrent()

        // Assert
        assertTrue(repository.insertedMovieCollectIds.isEmpty())
        assertEquals(listOf(1), repository.deletedMovieCollectIds)
    }

    @Test
    fun `搜尋結果清單含多部電影時，只切換其中一部收藏不影響其他電影`() = runTest(dispatcher) {
        // Arrange：模擬搜尋結果清單中同時顯示多部電影
        val repository = FakeMovieRepository()
        val viewModel = createViewModel(repository)
        val targetMovie = MovieCardData(
            movieCardId = 2,
            movieCardTitle = "B",
            movieCardPosterPath = "",
            movieCardReleaseDate = "2021-10-22",
            movieCardVoteAverage = 8.0,
            movieCardIsCollect = false,
            movieCardTimestamp = 0L,
        )

        // Act：只對目標電影觸發收藏切換
        viewModel.toggleMovieCollectStatus(targetMovie)
        runCurrent()

        // Assert：只新增目標電影的收藏紀錄，其餘電影 id 不受影響
        assertEquals(listOf(2), repository.insertedMovieCollectIds)
        assertTrue(repository.deletedMovieCollectIds.isEmpty())
    }

    @Test
    fun `已有搜尋關鍵字時 languageMode 變化，以相同關鍵字重新從第一頁呼叫 getSearchMovieListUseCase`() = runTest(dispatcher) {
        // Arrange
        val repository = FakeMovieRepository()
        val userDataRepository = FakeUserDataRepository()
        val viewModel = createViewModel(repository, userDataRepository)
        val job = viewModel.movieSearchPager.launchIn(this)
        viewModel.startSearch("Dune")
        advanceTimeBy(300)
        runCurrent()

        // Act
        userDataRepository.setLanguageMode(LanguageMode.ENGLISH)
        runCurrent()

        // Assert
        assertEquals(listOf("Dune", "Dune"), repository.searchQueries)
        job.cancel()
    }

    @Test
    fun `尚未輸入關鍵字時 languageMode 變化，MUST NOT 呼叫 getSearchMovieListUseCase`() = runTest(dispatcher) {
        // Arrange
        val repository = FakeMovieRepository()
        val userDataRepository = FakeUserDataRepository()
        val viewModel = createViewModel(repository, userDataRepository)
        val job = viewModel.movieSearchPager.launchIn(this)
        runCurrent()

        // Act
        userDataRepository.setLanguageMode(LanguageMode.ENGLISH)
        runCurrent()

        // Assert
        assertTrue(repository.searchQueries.isEmpty())
        job.cancel()
    }

    private fun createViewModel(
        repository: MovieRepository,
        userDataRepository: UserDataRepository = FakeUserDataRepository(),
    ): SearchViewModel =
        SearchViewModel(
            movieRepository = repository,
            userDataRepository = userDataRepository,
            getSearchMovieListUseCase = GetSearchMovieListUseCase(repository, dispatcher),
        )
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

private class FakeMovieRepository : MovieRepository {

    val searchQueries = mutableListOf<String>()
    val insertedMovieCollectIds = mutableListOf<Int>()
    val deletedMovieCollectIds = mutableListOf<Int>()

    override fun getConfiguration(): Flow<Result<ConfigurationBean>> = flowOf(Result.success(ConfigurationBean()))

    override fun getMovieGenres(): Flow<AppResult<MovieGenreBean>> = flowOf(AppResult.Success(MovieGenreBean()))

    override fun getMovieListPager(withGenres: String): Flow<PagingData<MovieCardResult>> = flowOf(PagingData.empty())

    override fun getMovieSearchPager(query: String): Flow<PagingData<MovieCardResult>> {
        searchQueries += query
        return flowOf(PagingData.empty())
    }

    override fun getMovieDetail(id: Int): Flow<Result<MovieDetailBean>> = flowOf(Result.success(MovieDetailBean()))

    override fun getMovieRecommendations(id: Int): Flow<Result<MovieRecommendBean>> =
        flowOf(Result.success(MovieRecommendBean()))

    override fun getMovieActor(id: Int): Flow<AppResult<MovieCastAndCrewBean>> =
        flowOf(AppResult.Success(MovieCastAndCrewBean()))

    override suspend fun insertMovieCollect(movieResult: MovieCardResult) {
        insertedMovieCollectIds += movieResult.id
    }

    override suspend fun deleteMovieCollect(movieResult: MovieCardResult) {
        deletedMovieCollectIds += movieResult.id
    }

    override fun getCollectedMovieIds(): Flow<List<Int>> = flowOf(emptyList())

    override fun getAllMovieCollect(): Flow<List<MovieCardResult>> = flowOf(emptyList())

    override fun getMovieCollectEntityById(id: Int): Flow<MovieCardResult?> = flowOf(null)

    override suspend fun insertMovieHistory(movieResult: MovieCardResult) = Unit

    override suspend fun deleteMovieHistory(movieResult: MovieCardResult) = Unit

    override fun getAllMovieHistory(): Flow<List<MovieCardResult>> = flowOf(emptyList())

    override suspend fun deleteAllMovieHistory(): Boolean = false
}
