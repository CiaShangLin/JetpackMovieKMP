## Purpose
定義 `shared/domain` 的電影相關 UseCase 對外契約，包含 configuration 載入、快取退回、收藏狀態標記與 Koin module 組裝行為。
## Requirements
### Requirement: Configuration UseCase 具備快取退回機制

`shared/domain` 的 `commonMain` SHALL 提供 `GetConfigurationUseCase`，整合 `shared:data` 的 `MovieRepository.getConfiguration()` 與 `UserDataRepository`，回傳型別為 `Flow<AppResult<ConfigurationBean>>`：API 成功時寫入本地快取並回傳 `AppResult.Success`；API 失敗時若本地有快取則回傳快取內容並視為 `AppResult.Success`，皆無快取才回傳 `AppResult.Failure`（原始錯誤轉換後的 `AppError`）。

#### Scenario: API 呼叫成功時寫入快取並回傳成功

- **WHEN** 呼叫 `GetConfigurationUseCase()` 且 `MovieRepository.getConfiguration()` 回傳 `Result.success(configuration)`
- **THEN** `UserDataRepository.setConfiguration(configuration)` 被呼叫，且回傳的 `Flow` emit `AppResult.Success(configuration)`

#### Scenario: API 呼叫失敗但本地有快取時退回快取

- **WHEN** 呼叫 `GetConfigurationUseCase()` 且 `MovieRepository.getConfiguration()` 回傳 `Result.failure(...)`，`UserDataRepository.userData` 目前的 configuration 不為 null
- **THEN** 回傳的 `Flow` emit `AppResult.Success(cachedConfiguration)`

#### Scenario: API 呼叫失敗且本地無快取時回傳原始錯誤

- **WHEN** 呼叫 `GetConfigurationUseCase()` 且 `MovieRepository.getConfiguration()` 回傳 `Result.failure(error)`，`UserDataRepository.userData` 目前的 configuration 為 null
- **THEN** 回傳的 `Flow` emit `AppResult.Failure(error.toAppError())`

### Requirement: 瀏覽紀錄 UseCase 標記收藏狀態

`shared/domain` 的 `commonMain` SHALL 提供 `GetHistoryMovieListUseCase`，合併 `shared:data` 的 `MovieRepository.getAllMovieHistory()` 與 `getCollectedMovieIds()`，將每筆瀏覽紀錄的 `isCollect` 更新為目前實際收藏狀態。

#### Scenario: 瀏覽紀錄中已收藏的電影標記為 true

- **WHEN** 呼叫 `GetHistoryMovieListUseCase()`，瀏覽紀錄包含某部電影且該電影 id 存在於 `getCollectedMovieIds()` 結果中
- **THEN** 回傳的 `Flow` emission 中該筆電影的 `isCollect` 為 `true`

#### Scenario: 瀏覽紀錄中未收藏的電影標記為 false

- **WHEN** 呼叫 `GetHistoryMovieListUseCase()`，瀏覽紀錄包含某部電影但該電影 id 不存在於 `getCollectedMovieIds()` 結果中
- **THEN** 回傳的 `Flow` emission 中該筆電影的 `isCollect` 為 `false`

### Requirement: 首頁電影清單 UseCase 標記收藏狀態

`shared/domain` 的 `commonMain` SHALL 提供 `GetHomeMovieListUseCase`，合併 `shared:data` 的 `MovieRepository.getMovieListPager(withGenres)` 分頁資料與 `getCollectedMovieIds()`，標記每筆分頁電影的 `isCollect`，並依呼叫端提供的 `CoroutineScope` 執行 `cachedIn`。

#### Scenario: 分頁電影清單標記收藏狀態

- **WHEN** 呼叫 `GetHomeMovieListUseCase(withGenres, scope)` 並收集分頁資料
- **THEN** 回傳的 `Flow<PagingData<MovieCardResult>>` 中每筆電影的 `isCollect` 反映 `getCollectedMovieIds()` 目前的結果

### Requirement: 電影詳情 UseCase 自動寫入瀏覽紀錄

`shared/domain` 的 `commonMain` SHALL 提供 `GetMovieDetailUseCase`，呼叫 `shared:data` 的 `MovieRepository.getMovieDetail(movieId)`，成功時額外呼叫 `insertMovieHistory(...)` 寫入瀏覽紀錄。

#### Scenario: 取得詳情成功時寫入瀏覽紀錄

- **WHEN** 呼叫 `GetMovieDetailUseCase(movieId)` 且 `MovieRepository.getMovieDetail(movieId)` 回傳 `Result.success(detail)`
- **THEN** `MovieRepository.insertMovieHistory(...)` 被呼叫一次，且回傳的 `Flow` emit `Result.success(detail)`

#### Scenario: 取得詳情失敗時不寫入瀏覽紀錄

- **WHEN** 呼叫 `GetMovieDetailUseCase(movieId)` 且 `MovieRepository.getMovieDetail(movieId)` 回傳 `Result.failure(...)`
- **THEN** `MovieRepository.insertMovieHistory(...)` 不會被呼叫，且回傳的 `Flow` emit `Result.failure(...)`

### Requirement: 電影推薦 UseCase 標記收藏狀態

`shared/domain` 的 `commonMain` SHALL 提供 `GetMovieRecommendUseCase`，合併 `shared:data` 的 `MovieRepository.getMovieRecommendations(movieId)` 與 `getCollectedMovieIds()`，標記推薦清單中每部電影的 `isCollect`。

#### Scenario: 推薦清單成功時標記收藏狀態

- **WHEN** 呼叫 `GetMovieRecommendUseCase(movieId)` 且 `MovieRepository.getMovieRecommendations(movieId)` 回傳 `Result.success(...)`
- **THEN** 回傳的 `Flow` emit `Result.success(recommendations)`，其中每部電影的 `isCollect` 反映 `getCollectedMovieIds()` 目前的結果

#### Scenario: 推薦清單失敗時回傳原始錯誤

- **WHEN** 呼叫 `GetMovieRecommendUseCase(movieId)` 且 `MovieRepository.getMovieRecommendations(movieId)` 回傳 `Result.failure(error)`
- **THEN** 回傳的 `Flow` emit `Result.failure(error)`

### Requirement: Domain Koin module

`shared/domain` MUST 提供 Koin `domainModule()`，可解析全部 5 個 UseCase，且不需要任何平台專屬參數（只依賴既有 `shared:data` 的 `dataModule()`／`shared:common` 的 `commonModule()` 已提供的元件）。

#### Scenario: domain module 可解析全部 5 個 UseCase

- **WHEN** 安裝 `commonModule()`、`networkModule(...)`、`databaseModule(...)`、`datastoreModule(...)`、`dataModule()`、`domainModule()` 後向 Koin container 要求 5 個 UseCase
- **THEN** 皆可成功 resolve，且不拋出 Koin `DefinitionResolutionException` 之類的錯誤

#### Scenario: initKoin 安裝 domainModule

- **WHEN** 呼叫 `shared:app` 提供的 `initKoin(...)`
- **THEN** 啟動後的 Koin container 可直接 resolve 5 個 UseCase，不需要呼叫端額外安裝其他 module

