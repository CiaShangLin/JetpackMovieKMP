# android-content-language-refresh Specification

## Purpose

定義 Android 端顯示 TMDB 遠端內容的 ViewModel（Home、Search、Detail）在使用者切換 App 內語言設定後，必須主動以新語言重新載入資料的驗收準則，涵蓋一次性資料與 Paging 分頁資料兩種情境。範圍限定於這些 ViewModel 本身的資料重新載入行為，不涵蓋 Navigation3 導覽狀態、`MainActivity` 靜態字串刷新機制，也不涵蓋 `feature/collect`／`feature/history` 這類讀取本地資料庫、不受目前語言即時影響的畫面。

## ADDED Requirements

### Requirement: `HomeViewModel` 的電影分類清單 MUST 於語言變更時重新載入

`HomeViewModel.movieGenres` MUST 訂閱 `UserDataRepository.userData` 的 `languageMode` 變化，當 `languageMode` 改變時 MUST 重新呼叫 `MovieRepository.getMovieGenres()`，且此行為 MUST 與既有的 `retry()` 觸發共用同一組資料載入流程。

#### Scenario: 使用者切換語言後電影分類清單以新語言重新載入

- **WHEN** 使用者在設定頁將 `LanguageMode` 由一種語言切換為另一種
- **THEN** `HomeViewModel.movieGenres` MUST 重新呼叫 `MovieRepository.getMovieGenres()`，並以新的語言結果更新 `HomeUiState`

#### Scenario: 主題變更不觸發電影分類清單重新載入

- **WHEN** 使用者僅變更 `ThemeMode`、`languageMode` 未改變
- **THEN** `HomeViewModel.movieGenres` MUST NOT 重新呼叫 `MovieRepository.getMovieGenres()`

### Requirement: `HomeContentViewModel` 的分頁片單 MUST 於語言變更時從第一頁重新載入

`HomeContentViewModel` MUST 注入 `UserDataRepository` 並訂閱 `languageMode` 變化；當 `languageMode` 改變時，`movieList` MUST 捨棄目前 `Pager` 的分頁快取，重新以新語言呼叫 `getMovieGenreUseCase` 從第一頁開始載入。

#### Scenario: 使用者在瀏覽 Home 分頁片單時切換語言

- **WHEN** 使用者已載入部分頁數的電影片單後切換 `languageMode`
- **THEN** `movieList` MUST 以新語言重新從第一頁開始載入，MUST NOT 沿用切換前已快取的分頁資料

### Requirement: `SearchViewModel` 的搜尋分頁結果 MUST 於語言變更時重新載入

`SearchViewModel` MUST 注入 `UserDataRepository` 並訂閱 `languageMode` 變化。當使用者已提交搜尋關鍵字時，`languageMode` 改變 MUST 觸發 `movieSearchPager` 以目前關鍵字重新從第一頁呼叫 `getSearchMovieListUseCase`；當尚未提交關鍵字（`searchQuery` 為空字串）時，`languageMode` 改變 MUST NOT 觸發任何搜尋請求。

#### Scenario: 已有搜尋關鍵字時切換語言

- **WHEN** 使用者已提交非空白搜尋關鍵字並取得分頁結果後切換 `languageMode`
- **THEN** `movieSearchPager` MUST 以相同關鍵字、新語言重新從第一頁呼叫 `getSearchMovieListUseCase`

#### Scenario: 尚未輸入關鍵字時切換語言

- **WHEN** 使用者尚未提交任何搜尋關鍵字（`searchQuery` 為空字串）時切換 `languageMode`
- **THEN** `movieSearchPager` MUST 維持回傳 `PagingData.empty()`，MUST NOT 呼叫 `getSearchMovieListUseCase`

### Requirement: `MovieDetailViewModel` 的電影詳情 MUST 於語言變更時重新載入

`MovieDetailViewModel` MUST 注入 `UserDataRepository` 並訂閱 `languageMode` 變化；當 `languageMode` 改變時，`movieDetail` MUST 重新呼叫 `getMovieDetailUseCase(movieId)`，此行為 MUST 與既有 `retryMovieDetail()` 觸發共用同一組資料載入流程。

#### Scenario: 使用者在電影詳情頁切換語言

- **WHEN** 使用者停留在 `MovieDetailScreen` 時切換 `languageMode`
- **THEN** `movieDetail` MUST 重新呼叫 `getMovieDetailUseCase(movieId)` 並以新語言結果更新 `MovieDetailUiState`

### Requirement: `MovieDetailViewModel` 的推薦電影與演員名單 MUST 於語言變更時重新載入

`movieRecommendations` 與 `movieActors` MUST 各自訂閱 `languageMode` 變化，當 `languageMode` 改變時 MUST 分別重新呼叫 `getMovieRecommendUseCase(movieId)`／`MovieRepository.getMovieActor(movieId)`；此新增行為 MUST NOT 影響 `retryMovieDetail()` 現有僅重試 `movieDetail` 的既定範圍。

#### Scenario: 語言變更時推薦電影重新載入

- **WHEN** 使用者停留在 `MovieDetailScreen` 時切換 `languageMode`
- **THEN** `movieRecommendations` MUST 重新呼叫 `getMovieRecommendUseCase(movieId)`

#### Scenario: 語言變更時演員名單重新載入

- **WHEN** 使用者停留在 `MovieDetailScreen` 時切換 `languageMode`
- **THEN** `movieActors` MUST 重新呼叫 `MovieRepository.getMovieActor(movieId)`

### Requirement: 語言變更觸發的資料重新載入 MUST NOT 影響 Navigation3 導覽狀態

本 capability 涵蓋的所有 ViewModel 重新載入行為，MUST NOT 導致 Navigation3 的 `backStack` 內容或深度改變，MUST NOT 觸發 NavEntry-scope ViewModel 被銷毀重建。

#### Scenario: 切換語言不影響導覽深度

- **WHEN** 使用者導覽至 `MovieDetailScreen`（`backStack` 深度大於 1）後切換 `languageMode`
- **THEN** `backStack` 的深度與內容 MUST 維持不變，MUST NOT 被重置回 `HomeKey`
