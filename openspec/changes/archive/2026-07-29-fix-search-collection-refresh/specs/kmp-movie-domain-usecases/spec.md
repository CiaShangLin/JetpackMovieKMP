## ADDED Requirements

### Requirement: 搜尋電影清單 UseCase 標記收藏狀態

`shared/domain` 的 `commonMain` SHALL 提供 `GetSearchMovieListUseCase`，合併 `MovieRepository.getMovieSearchPager(query)` 的分頁資料與 `getCollectedMovieIds()`，並將每筆搜尋結果的 `isCollect` 更新為目前實際收藏狀態。呼叫端 MUST 對最終搜尋資料流執行一次生命週期綁定的 Pager 快取。

#### Scenario: 搜尋結果中已收藏電影標記為 true

- **WHEN** 呼叫 `GetSearchMovieListUseCase(query)` 並收集資料，且某筆搜尋電影 id 存在於 `getCollectedMovieIds()` 結果
- **THEN** 回傳的 `Flow<PagingData<MovieCardResult>>` 中該筆電影的 `isCollect` SHALL 為 true

#### Scenario: 收藏資料變更後更新已載入搜尋結果

- **WHEN** `GetSearchMovieListUseCase` 已回傳搜尋結果，且 `getCollectedMovieIds()` 後續 emission 新增或移除其中一部電影 id
- **THEN** 後續搜尋 Paging emission 中對應電影的 `isCollect` SHALL 反映最新收藏狀態

## MODIFIED Requirements

### Requirement: Domain Koin module

`shared/domain` MUST 提供 Koin `domainModule()`，可解析全部 6 個 UseCase，且不需要任何平台專屬參數（只依賴既有 `shared:data` 的 `dataModule()`／`shared:common` 的 `commonModule()` 已提供的元件）。

#### Scenario: domain module 可解析全部 5 個 UseCase

- **WHEN** 安裝 `commonModule()`、`networkModule(...)`、`databaseModule(...)`、`datastoreModule(...)`、`dataModule()`、`domainModule()` 後向 Koin container 要求原有的 5 個 UseCase
- **THEN** 皆可成功 resolve，且不拋出 Koin `DefinitionResolutionException` 之類的錯誤

#### Scenario: domain module 可解析全部 6 個 UseCase

- **WHEN** 安裝 `commonModule()`、`networkModule(...)`、`databaseModule(...)`、`datastoreModule(...)`、`dataModule()`、`domainModule()` 後向 Koin container 要求 6 個 UseCase
- **THEN** 皆可成功 resolve，且不拋出 Koin `DefinitionResolutionException` 之類的錯誤

#### Scenario: initKoin 安裝 domainModule

- **WHEN** 呼叫 `shared:app` 提供的 `initKoin(...)`
- **THEN** 啟動後的 Koin container 可直接 resolve 6 個 UseCase，不需要呼叫端額外安裝其他 module
