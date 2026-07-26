package com.shang.jetpackmoviekmp.presenter

/**
 * [HomeMovieListPresenter] 對外暴露的載入狀態，取代直接暴露
 * `androidx.paging.LoadState`／`CombinedLoadStates`。
 *
 * `androidx.paging`（`paging-common`）是第三方依賴，並未列在 `shared/app`
 * 的 iOS framework `export()` 清單內，其型別無法直接跨到 Swift 使用；且
 * 該函式庫內部剛好存在名為 `DEBUG` 的成員，若整包 export 會與 Xcode Debug
 * build 的 `DEBUG` C 巨集衝突導致 header 編譯失敗，因此改為在此定義一組
 * 自有型別，由 [HomeMovieListPresenter] 內部轉換後再暴露給 Swift。
 */
sealed interface HomeMovieListLoadState {
    data object Idle : HomeMovieListLoadState
    data object Loading : HomeMovieListLoadState
    data class Error(val message: String) : HomeMovieListLoadState
}

/** [HomeMovieListPresenter.loadStateFlow] 對外暴露的整體載入狀態。 */
data class HomeMovieListLoadStates(
    val refresh: HomeMovieListLoadState,
    val append: HomeMovieListLoadState,
)
