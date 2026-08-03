package com.shang.jetpackmoviekmp

import com.shang.jetpackmoviekmp.common.BaseHostUrlProvider
import com.shang.jetpackmoviekmp.common.di.CommonDispatcher
import com.shang.jetpackmoviekmp.data.repository.MovieRepository
import com.shang.jetpackmoviekmp.data.repository.UserDataRepository
import com.shang.jetpackmoviekmp.domain.usecase.GetConfigurationUseCase
import com.shang.jetpackmoviekmp.domain.usecase.GetHistoryMovieListUseCase
import com.shang.jetpackmoviekmp.domain.usecase.GetHomeMovieListUseCase
import com.shang.jetpackmoviekmp.domain.usecase.GetMovieDetailUseCase
import com.shang.jetpackmoviekmp.domain.usecase.GetMovieRecommendUseCase
import com.shang.jetpackmoviekmp.domain.usecase.GetSearchMovieListUseCase
import com.shang.jetpackmoviekmp.presenter.HomeMovieListPresenter
import com.shang.jetpackmoviekmp.presenter.SearchMovieListPresenter
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.qualifier.named

/**
 * 提供 iOS 端可呼叫的 Koin 依賴橋接物件。
 *
 * `KoinHelper` 僅能在 `doInitKoinIos` 啟動 Koin 之後呼叫，否則 Koin 尚未建立全域
 * context 時會無法解析依賴。日後新增 Swift 端需要的依賴時，請在此物件新增具名
 * accessor，例如 `userDataRepository()`，避免將 reified generic API 直接暴露給 Swift。
 *
 * Swift feature View 可直接透過此物件取得需要的 shared 依賴並建立 ViewModel，避免由
 * `MainView`／`MainTab` 逐層轉送依賴。
 */
object KoinHelper : KoinComponent {

    fun getConfigurationUseCase(): GetConfigurationUseCase = getKoin().get()

    /**
     * 解析使用者偏好設定 repository，作為 iOS 端新增 accessor 的命名範例。
     */
    fun userDataRepository(): UserDataRepository = getKoin().get()

    /**
     * 解析電影詳情 UseCase，供 iOS 端消費 `Flow<AppResult<MovieDetailBean>>`。
     */
    fun getMovieDetailUseCase(): GetMovieDetailUseCase = getKoin().get()

    /** 解析電影推薦 UseCase，供 iOS 端消費 `Flow<AppResult<List<MovieCardResult>>>`。 */
    fun getMovieRecommendUseCase(): GetMovieRecommendUseCase = getKoin().get()

    /**
     * 解析 [MovieRepository]，供 iOS 端直接呼叫 `getMovieGenres()`
     * 取得可被 SKIE 匯出的 `Flow<AppResult<MovieGenreBean>>`。
     */
    fun getMovieRepository(): MovieRepository = getKoin().get()

    /**
     * 解析圖片 CDN base URL provider，供 iOS 圖片元件組裝 TMDB 相對圖片路徑。
     */
    fun getBaseHostUrlProvider(): BaseHostUrlProvider = getKoin().get()

    /**
     * 建立一個 iOS 專用的首頁電影清單分頁 Presenter，內部沿用既有
     * [GetHomeMovieListUseCase] 並自行管理 `CoroutineScope`。
     *
     * 每個 Genre 應各自呼叫一次取得獨立實例（比照 Android 每個分類分頁各自一個
     * `HomeContentViewModel`），呼叫端須在畫面消失時呼叫回傳物件的 `clear()`。
     *
     * @param withGenres 指定查詢的電影類型 id 字串
     */
    fun createHomeMovieListPresenter(withGenres: String): HomeMovieListPresenter =
        HomeMovieListPresenter(
            getHomeMovieListUseCase = getKoin().get<GetHomeMovieListUseCase>(),
            withGenres = withGenres,
            ioDispatcher = getKoin().get(qualifier = named(CommonDispatcher.IO)),
        )

    /**
     * 建立 iOS 專用的搜尋電影清單分頁 Presenter。
     *
     * 每次提交搜尋關鍵字都應建立獨立實例；呼叫端在替換或釋放畫面時必須呼叫
     * 回傳物件的 `clear()`。
     *
     * @param query 使用者提交的搜尋關鍵字。
     */
    fun createSearchMovieListPresenter(query: String): SearchMovieListPresenter =
        SearchMovieListPresenter(
            getSearchMovieListUseCase = getKoin().get<GetSearchMovieListUseCase>(),
            query = query,
            ioDispatcher = getKoin().get(qualifier = named(CommonDispatcher.IO)),
        )

    fun getHistoryMovieListUseCase(): GetHistoryMovieListUseCase = getKoin().get()
}
