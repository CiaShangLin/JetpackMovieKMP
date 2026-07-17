# kmp-movie-local-database Specification

## Purpose
TBD - created by archiving change migrate-database-to-commonmain. Update Purpose after archive.
## Requirements
### Requirement: Shared 本地電影資料庫

`shared/commonMain` 必須提供 Room-based 本地資料庫，持久化電影收藏（`MovieCollectEntity`）與瀏覽紀錄（`MovieHistoryEntity`），且不得要求建立獨立 Gradle database module。

#### Scenario: 新增收藏會被持久化

- **WHEN** 呼叫 `MovieCollectDao.insertMovieCollect(entity)`
- **THEN** 後續 `MovieCollectDao.getAllMovies()` 的 emission 包含該筆 entity

#### Scenario: 刪除收藏會反映在查詢結果

- **WHEN** 對已存在的收藏 entity 呼叫 `MovieCollectDao.deleteMovie(entity)`
- **THEN** 後續 `MovieCollectDao.getAllMovies()` 的 emission 不再包含該筆 entity

#### Scenario: 依 id 查詢單一收藏

- **WHEN** 呼叫 `MovieCollectDao.getMovieCollectEntityById(id)`，且該 id 對應的收藏已存在
- **THEN** emission 回傳該筆 entity；若不存在則回傳 `null`

#### Scenario: 新增瀏覽紀錄會被持久化

- **WHEN** 呼叫 `MovieHistoryDao.insertMovie(entity)`
- **THEN** 後續 `MovieHistoryDao.getAllMovies()` 的 emission 包含該筆 entity

#### Scenario: 清空所有瀏覽紀錄

- **WHEN** 呼叫 `MovieHistoryDao.deleteAllMovies()`
- **THEN** 後續 `MovieHistoryDao.getAllMovies()` 的 emission 為空 list

### Requirement: 平台感知資料庫建立方式

database implementation 必須在每個支援平台建立/開啟同一個邏輯本地資料庫，並將 platform-specific 檔案路徑邏輯留在 common 業務邏輯之外。

#### Scenario: Android 使用 app-owned database 目錄

- **WHEN** Android 建立 `RoomDatabase.Builder<AppDatabase>`
- **THEN** 資料庫檔案位於 app-owned database 目錄，且 DI 外部 caller 不需要傳入 raw file paths

#### Scenario: iOS 使用穩定 app document 路徑

- **WHEN** iOS 建立 `RoomDatabase.Builder<AppDatabase>`
- **THEN** 資料庫檔案使用適合 app restart 的穩定 app-owned document 路徑

### Requirement: Database Koin module

`shared` MUST 提供 Koin database module，接受一個建立 `RoomDatabase.Builder<AppDatabase>` 的 lambda（只在第一次真正 resolve `AppDatabase` 時才呼叫，維持 Koin `single{}` 的 lazy 語意），並可 resolve `AppDatabase`、`MovieCollectDao`、`MovieHistoryDao`。

#### Scenario: database module 可解析 AppDatabase

- **WHEN** Koin 安裝以有效的 builder lambda 建立的 database module
- **THEN** 可以 resolve `AppDatabase`

#### Scenario: database module 可解析收藏與瀏覽紀錄 DAO

- **WHEN** Koin 安裝以有效的 builder lambda 建立的 database module
- **THEN** 可以 resolve `MovieCollectDao` 與 `MovieHistoryDao`，且兩者背後為同一個 `AppDatabase` 實例
