## MODIFIED Requirements

### Requirement: Repository SHALL 提供收藏與瀏覽紀錄的本地讀寫

`shared/data` 的 `commonMain` SHALL 透過 `MovieRepository` 提供收藏與瀏覽紀錄的新增、刪除與 Flow 查詢。新增收藏時，`MovieRepository.insertMovieCollect(movieResult)` MUST 由 shared 層設定寫入當下的收藏時間，並寫入 `MovieCollectDao`；`getAllMovieCollect()` MUST 保留 DAO 的最新收藏優先順序，且每筆 emission 的 `isCollect` MUST 為 `true`。

#### Scenario: 新增電影收藏會寫入本地資料庫
- **WHEN** 對未收藏電影呼叫 `MovieRepository.insertMovieCollect(movieResult)`
- **THEN** `MovieCollectDao` MUST 新增帶有寫入當下時間的對應 `MovieCollectEntity`，後續 `MovieRepository.getAllMovieCollect()` 的 emission 包含該筆資料（`isCollect = true`）

#### Scenario: 已收藏電影依最新收藏優先回傳
- **WHEN** 新增兩筆以上收藏且它們的收藏時間不同
- **THEN** `MovieRepository.getAllMovieCollect()` MUST 先發出收藏時間較新的電影

#### Scenario: 移除電影收藏會反映在查詢結果
- **WHEN** 對已收藏的電影呼叫 `MovieRepository.deleteMovieCollect(movieResult)`
- **THEN** 後續 `MovieRepository.getAllMovieCollect()`／`getCollectedMovieIds()` 的 emission 不再包含該筆電影
