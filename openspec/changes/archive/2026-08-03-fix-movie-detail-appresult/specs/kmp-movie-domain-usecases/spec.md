## MODIFIED Requirements

### Requirement: 電影詳情 UseCase 自動寫入瀏覽紀錄

`shared/domain` 的 `commonMain` SHALL 提供 `GetMovieDetailUseCase`，呼叫 `shared:data` 的 `MovieRepository.getMovieDetail(movieId)`，成功時額外呼叫 `insertMovieHistory(...)` 寫入瀏覽紀錄。回傳型別 SHALL 為 `Flow<AppResult<MovieDetailBean>>`：`MovieRepository.getMovieDetail(movieId)` 本身維持回傳 `Result`，`GetMovieDetailUseCase` 內部負責轉換為 `AppResult`（成功轉 `AppResult.Success`，失敗轉 `AppResult.Failure(error.toAppError())`），使 iOS 端可透過 SKIE 明確解析成功／失敗分支。

#### Scenario: 取得詳情成功時寫入瀏覽紀錄

- **WHEN** 呼叫 `GetMovieDetailUseCase(movieId)` 且 `MovieRepository.getMovieDetail(movieId)` 回傳 `Result.success(detail)`
- **THEN** `MovieRepository.insertMovieHistory(...)` 被呼叫一次，且回傳的 `Flow` emit `AppResult.Success(detail)`

#### Scenario: 取得詳情失敗時不寫入瀏覽紀錄

- **WHEN** 呼叫 `GetMovieDetailUseCase(movieId)` 且 `MovieRepository.getMovieDetail(movieId)` 回傳 `Result.failure(error)`
- **THEN** `MovieRepository.insertMovieHistory(...)` 不會被呼叫，且回傳的 `Flow` emit `AppResult.Failure(error.toAppError())`

### Requirement: 電影推薦 UseCase 標記收藏狀態

`shared/domain` 的 `commonMain` SHALL 提供 `GetMovieRecommendUseCase`，合併 `shared:data` 的 `MovieRepository.getMovieRecommendations(movieId)` 與 `getCollectedMovieIds()`，標記推薦清單中每部電影的 `isCollect`。回傳型別 SHALL 為 `Flow<AppResult<List<MovieCardResult>>>`：`MovieRepository.getMovieRecommendations(movieId)` 本身維持回傳 `Result`，`GetMovieRecommendUseCase` 內部負責轉換為 `AppResult`（成功轉 `AppResult.Success`，失敗轉 `AppResult.Failure(error.toAppError())`），使 iOS 端可透過 SKIE 明確解析成功／失敗分支。

#### Scenario: 推薦清單成功時標記收藏狀態

- **WHEN** 呼叫 `GetMovieRecommendUseCase(movieId)` 且 `MovieRepository.getMovieRecommendations(movieId)` 回傳 `Result.success(...)`
- **THEN** 回傳的 `Flow` emit `AppResult.Success(recommendations)`，其中每部電影的 `isCollect` 反映 `getCollectedMovieIds()` 目前的結果

#### Scenario: 推薦清單失敗時回傳原始錯誤

- **WHEN** 呼叫 `GetMovieRecommendUseCase(movieId)` 且 `MovieRepository.getMovieRecommendations(movieId)` 回傳 `Result.failure(error)`
- **THEN** 回傳的 `Flow` emit `AppResult.Failure(error.toAppError())`
