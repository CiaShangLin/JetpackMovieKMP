package com.shang.jetpackmoviekmp.ui

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
 * [MovieRepository] 測試替身，`MainViewModel` 測試只用得到 [getConfiguration]，
 * 其餘方法不會被呼叫到，故回傳合理預設值即可。
 */
internal class FakeMovieRepository : MovieRepository {

    var configurationResult: Result<ConfigurationBean> = Result.success(ConfigurationBean())

    override fun getConfiguration(): Flow<Result<ConfigurationBean>> = flowOf(configurationResult)

    override fun getMovieGenres(): Flow<Result<MovieGenreBean>> = flowOf(Result.success(MovieGenreBean()))

    override fun getMovieListPager(withGenres: String): Flow<PagingData<MovieCardResult>> =
        flowOf(PagingData.empty())

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

/**
 * [UserDataRepository] 測試替身，`userData` 以 [MutableStateFlow] 模擬持久化狀態。
 * [userDataFlow] 可覆寫成 `emptyFlow()`，模擬本地完全無快取的情境。
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
