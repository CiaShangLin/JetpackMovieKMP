## MODIFIED Requirements

### Requirement: shared 本地資料庫 SHALL 持久化收藏與瀏覽紀錄

`shared/database` 的 `commonMain` MUST 提供 Room-based 本地資料庫，持久化電影收藏（`MovieCollectEntity`）與瀏覽紀錄（`MovieHistoryEntity`）。`MovieCollectDao.getAllMovies()` MUST 以收藏時間 `timestamp` 由新到舊回傳收藏資料；當時間相同時 MUST 使用電影 id 作為確定性的次要排序。

#### Scenario: 新增收藏會被持久化
- **WHEN** 呼叫 `MovieCollectDao.insertMovieCollect(entity)`
- **THEN** 後續 `MovieCollectDao.getAllMovies()` 的 emission 包含該筆 entity

#### Scenario: 刪除收藏會反映在查詢結果
- **WHEN** 對已存在的收藏 entity 呼叫 `MovieCollectDao.deleteMovie(entity)`
- **THEN** 後續 `MovieCollectDao.getAllMovies()` 的 emission 不再包含該筆 entity

#### Scenario: 依 id 查詢單一收藏
- **WHEN** 呼叫 `MovieCollectDao.getMovieCollectEntityById(id)`，且該 id 對應的收藏已存在
- **THEN** Flow MUST 發出該筆 `MovieCollectEntity`

#### Scenario: 收藏依最新時間排序
- **WHEN** 收藏 DAO 有兩筆以上 timestamp 不同的收藏資料
- **THEN** `getAllMovies()` MUST 先發出 timestamp 較大的收藏資料

#### Scenario: 收藏時間相同時順序可預期
- **WHEN** 收藏 DAO 有兩筆 timestamp 相同且 id 不同的收藏資料
- **THEN** `getAllMovies()` MUST 依電影 id 的既定次要排序回傳資料
