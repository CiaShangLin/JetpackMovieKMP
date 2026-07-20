## MODIFIED Requirements

### Requirement: Shared 本地電影資料庫

`shared/database` 的 `commonMain` MUST 提供 Room-based 本地資料庫，持久化電影收藏（`MovieCollectEntity`）與瀏覽紀錄（`MovieHistoryEntity`）。

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

### Requirement: Database Koin module

`shared/database` MUST 提供 Koin database module，接受一個建立 `RoomDatabase.Builder<AppDatabase>` 的 lambda（只在第一次真正 resolve `AppDatabase` 時才呼叫，維持 Koin `single{}` 的 lazy 語意），並可 resolve `AppDatabase`、`MovieCollectDao`、`MovieHistoryDao`。

#### Scenario: database module 可解析 AppDatabase

- **WHEN** Koin 安裝以有效的 builder lambda 建立的 database module
- **THEN** 可以 resolve `AppDatabase`

#### Scenario: database module 可解析收藏與瀏覽紀錄 DAO

- **WHEN** Koin 安裝以有效的 builder lambda 建立的 database module
- **THEN** 可以 resolve `MovieCollectDao` 與 `MovieHistoryDao`，且兩者背後為同一個 `AppDatabase` 實例
