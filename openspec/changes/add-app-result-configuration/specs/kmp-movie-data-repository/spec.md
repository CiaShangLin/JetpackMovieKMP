## MODIFIED Requirements

### Requirement: 電影資料 Repository

`shared/data` 的 `commonMain` SHALL 對外提供 `MovieRepository` 作為電影資料存取介面，整合 `MovieDataSource`（`shared:network`）與本地資料庫（`shared:database` 的 `MovieCollectDao`／`MovieHistoryDao`），對外提供 configuration、電影類型、電影分頁列表／搜尋、電影詳情／推薦／演員陣容，以及收藏／瀏覽紀錄的讀寫。

`MovieRepository` 的 public API SHALL 只暴露 model 型別、`Flow`／`Result`／`AppResult`（`getConfiguration()` 專用）與 paging public 型別，不得暴露 `shared:network`、`shared:database`、`shared:datastore` 的 datasource、DAO、entity 或 DataStore 實作型別。`MovieRepositoryImpl`、paging source 與 model/entity mapper SHALL 視為 `shared:data` 內部實作細節，不得作為跨 module public API 使用。

#### Scenario: 取得 TMDB configuration

- **WHEN** 呼叫 `MovieRepository.getConfiguration()`
- **THEN** 回傳的 `Flow` emit `AppResult.Success(ConfigurationBean)`（成功）或 `AppResult.Failure(AppError.Network(...))`（`MovieDataSource` 回傳失敗時）

#### Scenario: 依類型分頁載入電影列表

- **WHEN** 呼叫 `MovieRepository.getMovieListPager(withGenres)` 並收集分頁資料
- **THEN** 回傳的 `Flow<PagingData<MovieCardResult>>` 依序載入 `MovieDataSource.getDiscoverMovie(withGenres, page)` 的分頁結果，分頁邊界（`prevKey`／`nextKey`）依 `totalPages` 正確計算

#### Scenario: 依關鍵字分頁搜尋電影

- **WHEN** 呼叫 `MovieRepository.getMovieSearchPager(query)` 並收集分頁資料
- **THEN** 回傳的 `Flow<PagingData<MovieCardResult>>` 依序載入 `MovieDataSource.getMovieSearch(query, page)` 的分頁結果

#### Scenario: 新增電影收藏會寫入本地資料庫

- **WHEN** 呼叫 `MovieRepository.insertMovieCollect(movieResult)`
- **THEN** `MovieCollectDao` 新增對應的 `MovieCollectEntity`，後續 `MovieRepository.getAllMovieCollect()` 的 emission 包含該筆資料（`isCollect = true`）

#### Scenario: 移除電影收藏會反映在查詢結果

- **WHEN** 對已收藏的電影呼叫 `MovieRepository.deleteMovieCollect(movieResult)`
- **THEN** 後續 `MovieRepository.getAllMovieCollect()`／`getCollectedMovieIds()` 的 emission 不再包含該筆電影

#### Scenario: 新增瀏覽紀錄會寫入本地資料庫

- **WHEN** 呼叫 `MovieRepository.insertMovieHistory(movieResult)`
- **THEN** `MovieHistoryDao` 新增對應的 `MovieHistoryEntity`，後續 `MovieRepository.getAllMovieHistory()` 的 emission 包含該筆資料

#### Scenario: 清空所有瀏覽紀錄

- **WHEN** 呼叫 `MovieRepository.deleteAllMovieHistory()`
- **THEN** 回傳值反映是否有資料被刪除（有刪除回傳 `true`），且後續 `MovieRepository.getAllMovieHistory()` 的 emission 為空 list

#### Scenario: MovieRepository public API 不暴露底層型別

- **WHEN** 檢查 `MovieRepository` 的 public function signature
- **THEN** signature 不包含 `MovieDataSource`、DAO、database entity 或 DataStore datasource 型別
- **AND** 消費端只需依賴 repository interface 即可使用電影資料功能

#### Scenario: MovieRepository 實作由 dataModule 建立

- **WHEN** 安裝 `dataModule()` 並向 Koin container resolve `MovieRepository`
- **THEN** Koin 在 `shared:data` module 內部建構 repository implementation
- **AND** 呼叫端不需要也不能依賴 `MovieRepositoryImpl` 的具體 constructor
