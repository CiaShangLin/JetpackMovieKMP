package com.shang.jetpackmoviekmp.domain

import androidx.paging.PagingData
import com.shang.jetpackmoviekmp.data.repository.MovieRepository
import com.shang.jetpackmoviekmp.data.repository.UserDataRepository
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

/**
 * [MovieRepository] 測試替身，只有 `domain` UseCase 實際會用到的方法可透過對應
 * `xxxResponse`／`xxxFlow` 屬性客製化回傳值，其餘方法回傳合理預設值（空 Flow／預設
 * model），並記錄呼叫次數供測試驗證副作用（例如 [insertMovieHistory]）。
 *
 * 與 `data.repository.FakeMovieDataSource` 不同層級（後者是 network 層替身），
 * `domain` UseCase 依賴的是 [MovieRepository] 介面本身，故另外建立此替身。
 */
internal class FakeMovieRepository : MovieRepository {

    var configurationResult: Result<ConfigurationBean> = Result.success(ConfigurationBean())
    var movieDetailResult: Result<MovieDetailBean> = Result.success(MovieDetailBean())
    var movieRecommendationsResult: Result<MovieRecommendBean> = Result.success(MovieRecommendBean())
    var movieListPager: Flow<PagingData<MovieCardResult>> = flowOf(PagingData.empty())

    val movieHistoryFlow = MutableStateFlow<List<MovieCardResult>>(emptyList())
    val collectedMovieIdsFlow = MutableStateFlow<List<Int>>(emptyList())

    var insertMovieHistoryCallCount: Int = 0
        private set
    var lastInsertedMovieHistory: MovieCardResult? = null
        private set

    /** 設定後，下一次 [insertMovieHistory] 呼叫會拋出此例外，用來模擬瀏覽紀錄寫入失敗的情境。 */
    var insertMovieHistoryThrows: Throwable? = null

    override fun getConfiguration(): Flow<Result<ConfigurationBean>> = flowOf(configurationResult)

    override fun getMovieGenres(): Flow<Result<MovieGenreBean>> = flowOf(Result.success(MovieGenreBean()))

    override fun getMovieListPager(withGenres: String): Flow<PagingData<MovieCardResult>> = movieListPager

    override fun getMovieSearchPager(query: String): Flow<PagingData<MovieCardResult>> = flowOf(PagingData.empty())

    override fun getMovieDetail(id: Int): Flow<Result<MovieDetailBean>> = flowOf(movieDetailResult)

    override fun getMovieRecommendations(id: Int): Flow<Result<MovieRecommendBean>> =
        flowOf(movieRecommendationsResult)

    override fun getMovieActor(id: Int): Flow<Result<MovieCastAndCrewBean>> =
        flowOf(Result.success(MovieCastAndCrewBean()))

    override suspend fun insertMovieCollect(movieResult: MovieCardResult) = Unit

    override suspend fun deleteMovieCollect(movieResult: MovieCardResult) = Unit

    override fun getCollectedMovieIds(): Flow<List<Int>> = collectedMovieIdsFlow

    override fun getAllMovieCollect(): Flow<List<MovieCardResult>> = flowOf(emptyList())

    override fun getMovieCollectEntityById(id: Int): Flow<MovieCardResult?> = flowOf(null)

    override suspend fun insertMovieHistory(movieResult: MovieCardResult) {
        insertMovieHistoryCallCount++
        lastInsertedMovieHistory = movieResult
        insertMovieHistoryThrows?.let { throw it }
    }

    override suspend fun deleteMovieHistory(movieResult: MovieCardResult) = Unit

    override fun getAllMovieHistory(): Flow<List<MovieCardResult>> = movieHistoryFlow

    override suspend fun deleteAllMovieHistory(): Boolean = false
}

/**
 * [UserDataRepository] 測試替身，`userData` 預設用 [MutableStateFlow] 模擬持久化狀態，
 * `setXxx` 方法會直接更新該 flow。[userDataFlow] 可覆寫成 `emptyFlow()`，模擬
 * `GetConfigurationUseCase` 「本地完全無快取」（`firstOrNull()` 回傳 null）的情境。
 */
internal class FakeUserDataRepository(
    initial: UserData = UserData.getDefault(),
) : UserDataRepository {

    private val state = MutableStateFlow(initial)

    var userDataFlow: Flow<UserData> = state

    override val userData: Flow<UserData> get() = userDataFlow

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
