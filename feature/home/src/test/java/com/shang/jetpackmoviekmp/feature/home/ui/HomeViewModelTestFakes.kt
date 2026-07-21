package com.shang.jetpackmoviekmp.feature.home.ui

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
import java.util.concurrent.CountDownLatch

/**
 * [MovieRepository] 測試替身。`HomeViewModel`／`HomeContentViewModel` 測試只用得到
 * [getMovieGenres]、[getMovieListPager]、[insertMovieCollect]、[deleteMovieCollect]，
 * 其餘方法回傳合理預設值即可。
 *
 * [insertMovieCollect]／[deleteMovieCollect] 會在 `viewModelScope.launch(Dispatchers.IO)`
 * 的真實 IO 執行緒上被呼叫，用 [CountDownLatch] 讓測試可以確定性地等待呼叫完成，
 * 避免用 `Thread.sleep` 猜測時間造成 flaky test。
 */
internal class FakeMovieRepository : MovieRepository {

    var movieGenresResult: Result<MovieGenreBean> = Result.success(MovieGenreBean())
    var movieListPager: Flow<PagingData<MovieCardResult>> = flowOf(PagingData.empty())

    var insertMovieCollectCallCount: Int = 0
        private set
    var deleteMovieCollectCallCount: Int = 0
        private set

    val insertMovieCollectLatch = CountDownLatch(1)
    val deleteMovieCollectLatch = CountDownLatch(1)

    override fun getConfiguration(): Flow<Result<ConfigurationBean>> = flowOf(Result.success(ConfigurationBean()))

    override fun getMovieGenres(): Flow<Result<MovieGenreBean>> = flowOf(movieGenresResult)

    override fun getMovieListPager(withGenres: String): Flow<PagingData<MovieCardResult>> = movieListPager

    override fun getMovieSearchPager(query: String): Flow<PagingData<MovieCardResult>> = flowOf(PagingData.empty())

    override fun getMovieDetail(id: Int): Flow<Result<MovieDetailBean>> = flowOf(Result.success(MovieDetailBean()))

    override fun getMovieRecommendations(id: Int): Flow<Result<MovieRecommendBean>> =
        flowOf(Result.success(MovieRecommendBean()))

    override fun getMovieActor(id: Int): Flow<Result<MovieCastAndCrewBean>> =
        flowOf(Result.success(MovieCastAndCrewBean()))

    override suspend fun insertMovieCollect(movieResult: MovieCardResult) {
        insertMovieCollectCallCount++
        insertMovieCollectLatch.countDown()
    }

    override suspend fun deleteMovieCollect(movieResult: MovieCardResult) {
        deleteMovieCollectCallCount++
        deleteMovieCollectLatch.countDown()
    }

    override fun getCollectedMovieIds(): Flow<List<Int>> = flowOf(emptyList())

    override fun getAllMovieCollect(): Flow<List<MovieCardResult>> = flowOf(emptyList())

    override fun getMovieCollectEntityById(id: Int): Flow<MovieCardResult?> = flowOf(null)

    override suspend fun insertMovieHistory(movieResult: MovieCardResult) = Unit

    override suspend fun deleteMovieHistory(movieResult: MovieCardResult) = Unit

    override fun getAllMovieHistory(): Flow<List<MovieCardResult>> = flowOf(emptyList())

    override suspend fun deleteAllMovieHistory(): Boolean = false
}

/**
 * [UserDataRepository] 測試替身。`HomeViewModel` 目前建構時注入但未實際使用，
 * 提供最小可用實作即可。
 */
internal class FakeUserDataRepository(
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
