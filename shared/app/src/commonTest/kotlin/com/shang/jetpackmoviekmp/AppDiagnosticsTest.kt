package com.shang.jetpackmoviekmp

import androidx.paging.PagingData
import com.shang.jetpackmoviekmp.data.repository.MovieRepository
import com.shang.jetpackmoviekmp.data.repository.UserDataRepository
import com.shang.jetpackmoviekmp.domain.usecase.GetConfigurationUseCase
import com.shang.jetpackmoviekmp.model.ConfigurationBean
import com.shang.jetpackmoviekmp.model.LanguageMode
import com.shang.jetpackmoviekmp.model.MovieCardResult
import com.shang.jetpackmoviekmp.model.MovieCastAndCrewBean
import com.shang.jetpackmoviekmp.model.MovieDetailBean
import com.shang.jetpackmoviekmp.model.MovieGenreBean
import com.shang.jetpackmoviekmp.model.MovieRecommendBean
import com.shang.jetpackmoviekmp.model.ThemeMode
import com.shang.jetpackmoviekmp.model.UserData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AppDiagnosticsTest {

    private val testDispatcher = StandardTestDispatcher()
    private val movieRepository = FakeMovieRepository()
    private val userDataRepository = FakeUserDataRepository()
    private val appDiagnostics = AppDiagnostics(
        getConfigurationUseCase = GetConfigurationUseCase(
            movieRepository = movieRepository,
            userDataRepository = userDataRepository,
            ioDispatcher = testDispatcher,
        ),
        userDataRepository = userDataRepository,
    )

    @Test
    fun loadConfigurationSummary_returns_configuration_result_string() = runTest(testDispatcher) {
        // Given
        val configuration = ConfigurationBean(changeKeys = listOf("images"))
        movieRepository.configurationResult = Result.success(configuration)

        // When
        val summary = appDiagnostics.loadConfigurationSummary()

        // Then
        assertEquals(
            expected = Result.success(configuration).toString(),
            actual = summary,
            message = "應回傳 configuration use case 的結果摘要",
        )
    }

    @Test
    fun setLanguage_persists_english_when_useEnglish_is_true() = runTest(testDispatcher) {
        // When
        val language = appDiagnostics.setLanguage(useEnglish = true)

        // Then
        assertEquals(
            expected = LanguageMode.ENGLISH,
            actual = userDataRepository.lastLanguageMode,
            message = "useEnglish 為 true 時應持久化英文語言模式",
        )
        assertEquals(
            expected = LanguageMode.ENGLISH.toString(),
            actual = language,
            message = "應回傳已套用的英文語言名稱",
        )
    }

    @Test
    fun setLanguage_persists_traditional_chinese_when_useEnglish_is_false() = runTest(testDispatcher) {
        // When
        val language = appDiagnostics.setLanguage(useEnglish = false)

        // Then
        assertEquals(
            expected = LanguageMode.TRADITIONAL_CHINESE,
            actual = userDataRepository.lastLanguageMode,
            message = "useEnglish 為 false 時應持久化繁體中文語言模式",
        )
        assertEquals(
            expected = LanguageMode.TRADITIONAL_CHINESE.toString(),
            actual = language,
            message = "應回傳已套用的繁體中文語言名稱",
        )
    }
}

private class FakeMovieRepository : MovieRepository {
    var configurationResult: Result<ConfigurationBean> = Result.success(ConfigurationBean())

    override fun getConfiguration(): Flow<Result<ConfigurationBean>> = flowOf(configurationResult)

    override fun getMovieGenres(): Flow<Result<MovieGenreBean>> = flowOf(Result.success(MovieGenreBean()))

    override fun getMovieListPager(withGenres: String): Flow<PagingData<MovieCardResult>> = flowOf(PagingData.empty())

    override fun getMovieSearchPager(query: String): Flow<PagingData<MovieCardResult>> = flowOf(PagingData.empty())

    override fun getMovieDetail(id: Int): Flow<Result<MovieDetailBean>> = flowOf(Result.success(MovieDetailBean()))

    override fun getMovieRecommendations(id: Int): Flow<Result<MovieRecommendBean>> =
        flowOf(Result.success(MovieRecommendBean()))

    override fun getMovieActor(id: Int): Flow<Result<MovieCastAndCrewBean>> =
        flowOf(Result.success(MovieCastAndCrewBean()))

    override suspend fun insertMovieCollect(movieResult: MovieCardResult) = Unit

    override suspend fun deleteMovieCollect(movieResult: MovieCardResult) = Unit

    override fun getCollectedMovieIds(): Flow<List<Int>> = flowOf(emptyList())

    override fun getAllMovieCollect(): Flow<List<MovieCardResult>> = flowOf(emptyList())

    override fun getMovieCollectEntityById(id: Int): Flow<MovieCardResult?> = flowOf(null)

    override suspend fun insertMovieHistory(movieResult: MovieCardResult) = Unit

    override suspend fun deleteMovieHistory(movieResult: MovieCardResult) = Unit

    override fun getAllMovieHistory(): Flow<List<MovieCardResult>> = flowOf(emptyList())

    override suspend fun deleteAllMovieHistory(): Boolean = false
}

private class FakeUserDataRepository : UserDataRepository {
    private val state = MutableStateFlow(UserData.getDefault())

    var lastLanguageMode: LanguageMode? = null
        private set

    override val userData: Flow<UserData> = state

    override suspend fun setConfiguration(configuration: ConfigurationBean) {
        state.value = state.value.copy(configuration = configuration)
    }

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        state.value = state.value.copy(themeMode = themeMode)
    }

    override suspend fun setLanguageMode(languageMode: LanguageMode) {
        lastLanguageMode = languageMode
        state.value = state.value.copy(languageMode = languageMode)
    }
}
