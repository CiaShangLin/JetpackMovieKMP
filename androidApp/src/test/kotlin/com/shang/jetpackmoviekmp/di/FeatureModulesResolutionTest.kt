package com.shang.jetpackmoviekmp.di

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.room.Room
import androidx.room.RoomDatabase
import com.shang.jetpackmoviekmp.common.di.commonModule
import com.shang.jetpackmoviekmp.data.di.dataModule
import com.shang.jetpackmoviekmp.database.AppDatabase
import com.shang.jetpackmoviekmp.database.di.databaseModule
import com.shang.jetpackmoviekmp.datastore.di.datastoreModule
import com.shang.jetpackmoviekmp.domain.di.domainModule
import com.shang.jetpackmoviekmp.feature.collect.di.collectModule
import com.shang.jetpackmoviekmp.feature.collect.ui.CollectViewModel
import com.shang.jetpackmoviekmp.feature.detail.di.detailModule
import com.shang.jetpackmoviekmp.feature.detail.ui.MovieDetailViewModel
import com.shang.jetpackmoviekmp.feature.history.di.historyModule
import com.shang.jetpackmoviekmp.feature.history.ui.HistoryViewModel
import com.shang.jetpackmoviekmp.feature.home.di.homeModule
import com.shang.jetpackmoviekmp.feature.home.ui.HomeContentViewModel
import com.shang.jetpackmoviekmp.feature.home.ui.HomeViewModel
import com.shang.jetpackmoviekmp.feature.search.di.searchModule
import com.shang.jetpackmoviekmp.feature.search.ui.SearchViewModel
import com.shang.jetpackmoviekmp.feature.setting.di.settingModule
import com.shang.jetpackmoviekmp.feature.setting.ui.SettingViewModel
import com.shang.jetpackmoviekmp.model.MovieGenreBean
import com.shang.jetpackmoviekmp.network.di.networkModule
import com.shang.jetpackmoviekmp.ui.MainViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.parameter.parametersOf
import org.koin.test.KoinTest
import org.koin.test.inject
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * 驗證 `JetpackMovieApplication`（eager：[mainModule]、[homeModule]）與 `MainActivity.mainEntry()`
 * 延遲載入的 [collectModule]、[historyModule]、[searchModule]、[detailModule]、[settingModule]
 * 加總在一起時，Koin 依賴圖可完整解析。
 *
 * 不含 `uiModule`：其 `ImageLoader` 定義需要真實 Android [android.content.Context]，
 * 純 JVM 測試無法提供，改由 Task 5 的手動點擊驗證涵蓋。
 */
class FeatureModulesResolutionTest : KoinTest {

    private val mainViewModel: MainViewModel by inject()
    private val homeViewModel: HomeViewModel by inject()
    private val collectViewModel: CollectViewModel by inject()
    private val historyViewModel: HistoryViewModel by inject()
    private val searchViewModel: SearchViewModel by inject()
    private val settingViewModel: SettingViewModel by inject()

    @BeforeTest
    fun setUp() {
        startKoin {
            modules(
                commonModule(),
                datastoreModule(InMemoryPreferencesDataStore()),
                databaseModule { getInMemoryDatabaseBuilder() },
                networkModule(isDebug = true),
                dataModule(),
                domainModule(),
                mainModule(),
                homeModule(),
                collectModule(),
                historyModule(),
                searchModule(),
                detailModule(),
                settingModule(),
            )
        }
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun featureModules_resolve_mainViewModel() {
        assertNotNull(mainViewModel)
    }

    @Test
    fun featureModules_resolve_homeViewModel() {
        assertNotNull(homeViewModel)
    }

    @Test
    fun featureModules_resolve_homeContentViewModel() {
        val homeContentViewModel: HomeContentViewModel by inject { parametersOf(MovieGenreBean.MovieGenre()) }
        assertNotNull(homeContentViewModel)
    }

    @Test
    fun featureModules_resolve_collectViewModel() {
        assertNotNull(collectViewModel)
    }

    @Test
    fun featureModules_resolve_historyViewModel() {
        assertNotNull(historyViewModel)
    }

    @Test
    fun featureModules_resolve_searchViewModel() {
        assertNotNull(searchViewModel)
    }

    @Test
    fun featureModules_resolve_settingViewModel() {
        assertNotNull(settingViewModel)
    }

    @Test
    fun featureModules_resolve_movieDetailViewModel() {
        val movieDetailViewModel: MovieDetailViewModel by inject { parametersOf(1) }
        assertNotNull(movieDetailViewModel)
    }
}

/** In-memory [DataStore] 測試替身，不落地檔案系統。 */
private class InMemoryPreferencesDataStore : DataStore<Preferences> {

    private val state = MutableStateFlow<Preferences>(emptyPreferences())

    override val data = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        val updated = transform(state.value)
        state.value = updated
        return updated
    }
}

/** In-memory [AppDatabase] builder，供純 JVM 測試使用，不需要真實裝置。 */
private fun getInMemoryDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> =
    Room.inMemoryDatabaseBuilder<AppDatabase>(context = Application())
        .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
