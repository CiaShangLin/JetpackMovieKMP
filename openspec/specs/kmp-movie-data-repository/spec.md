# kmp-movie-data-repository Specification

## Purpose
TBD - created by archiving change migrate-data-to-commonmain. Update Purpose after archive.

## Requirements
### Requirement: 電影資料 Repository

`shared/commonMain` SHALL 提供 `MovieRepository`，整合 `MovieDataSource`（TMDB network）與本地資料庫（`MovieCollectDao`／`MovieHistoryDao`），對外提供 configuration、電影類型、電影分頁列表／搜尋、電影詳情／推薦／演員陣容，以及收藏／瀏覽紀錄的讀寫，且不得要求建立獨立 Gradle data module。

#### Scenario: 取得 TMDB configuration

- **WHEN** 呼叫 `MovieRepository.getConfiguration()`
- **THEN** 回傳的 `Flow` emit `Result.success(ConfigurationBean)`（成功）或 `Result.failure(...)`（`MovieDataSource` 回傳失敗時）

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

### Requirement: 使用者偏好設定 Repository

`shared/commonMain` SHALL 提供 `UserDataRepository`，包裝 `UserPreferenceDataSource`，對外以 `UserData`（configuration、theme、language 的聚合）暴露單一 `Flow`，並提供各別欄位的持久化方法。

#### Scenario: 觀察使用者偏好設定

- **WHEN** 收集 `UserDataRepository.userData`
- **THEN** emission 反映 `UserPreferenceDataSource` 目前持久化的 configuration／theme／language，尚未設定的欄位以 `UserData.getDefault()` 補齊

#### Scenario: 持久化 TMDB configuration

- **WHEN** 呼叫 `UserDataRepository.setConfiguration(configuration)`
- **THEN** 後續 `UserDataRepository.userData` 的 emission 反映新的 configuration

#### Scenario: 持久化主題模式

- **WHEN** 呼叫 `UserDataRepository.setThemeMode(themeMode)`
- **THEN** 後續 `UserDataRepository.userData` 的 emission 反映新的 `themeMode`

#### Scenario: 持久化語言模式

- **WHEN** 呼叫 `UserDataRepository.setLanguageMode(languageMode)`
- **THEN** 後續 `UserDataRepository.userData` 的 emission 反映新的 `languageMode`

### Requirement: Data Koin module

`shared` MUST 提供 Koin `dataModule()`，可解析 `MovieRepository`、`UserDataRepository`，且不需要任何平台專屬參數（只依賴既有 `networkModule`／`databaseModule`／`datastoreModule` 已提供的元件）。

#### Scenario: data module 可解析 MovieRepository 與 UserDataRepository

- **WHEN** 安裝 `commonModule()`、`networkModule(...)`、`databaseModule(...)`、`datastoreModule(...)`、`dataModule()` 後向 Koin container 要求 `MovieRepository`／`UserDataRepository`
- **THEN** 皆可成功 resolve，且不拋出 Koin `DefinitionResolutionException` 之類的錯誤

#### Scenario: initKoin 安裝 dataModule

- **WHEN** 呼叫 `initKoin(...)`
- **THEN** 啟動後的 Koin container 可直接 resolve `MovieRepository`／`UserDataRepository`，不需要呼叫端額外安裝其他 module
