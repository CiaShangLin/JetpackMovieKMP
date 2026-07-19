## Why

`JetpackMovieCompose/core/domain` 是參考專案的 UseCase 層，整合 `core/data` 的 `MovieRepository`／`UserDataRepository`，對外提供 5 個 UseCase（`GetConfigurationUseCase`、`GetHistoryMovieListUseCase`、`GetHomeMovieListUseCase`、`GetMovieDetailUseCase`、`GetMovieRecommendUseCase`），封裝「API 失敗時退回本地快取 configuration」「電影清單／推薦合併收藏狀態（`isCollect`）」「查看詳情時自動寫入瀏覽紀錄」等跨 Repository 的業務邏輯，並用 Hilt `@Inject constructor` 注入。目前 KMP 專案的 `shared/commonMain` 已遷移完成 `network`（`ktor-movie-network`）、`database`（`kmp-movie-local-database`）、`datastore`（`kmp-user-preferences-datastore`）、`data`（`kmp-movie-data-repository`，`MovieRepository`／`UserDataRepository`）四層，但 UI 層若要取得「已標記收藏狀態的電影清單」或「API 失敗自動退回快取的 configuration」，必須自行組合 `MovieRepository`／`UserDataRepository` 的多次呼叫，重複參考專案已經寫好的 UseCase 邏輯。

這次變更把 `core/domain` 遷移到 `shared/src/commonMain/kotlin/com/shang/jetpackmoviekmp/domain`，沿用先前 `data`／`database`／`datastore` 遷移的既有模式：commonMain 定義 UseCase class、Koin module 對外提供綁定，並比照既有 `initKoin` 串接方式安裝新 module。這次僅遷移到 `commonMain`，不建立獨立的 Gradle module（例如 `core:domain`）；Module 化留待之後的 change 處理。

## What Changes

- 在 `shared/src/commonMain/kotlin/com/shang/jetpackmoviekmp/` 下新增 `domain` package，包含 `usecase`、`di` 子 package。
- 將參考專案的 5 個 UseCase 依樣遷移到 KMP（行為與參考專案等價，僅把 Hilt `@Inject constructor` 換成一般 constructor、由 Koin 提供依賴）：
  - `GetConfigurationUseCase`：呼叫 `MovieRepository.getConfiguration()`，成功時寫入 `UserDataRepository.setConfiguration(...)`；失敗時退回 `UserDataRepository.userData` 目前快取的 configuration，皆無快取才回傳原始錯誤。
  - `GetHistoryMovieListUseCase`：合併 `MovieRepository.getAllMovieHistory()` 與 `getCollectedMovieIds()`，標記每筆瀏覽紀錄的 `isCollect`。
  - `GetHomeMovieListUseCase`：合併 `MovieRepository.getMovieListPager(withGenres)`（`cachedIn` 呼叫端提供的 `CoroutineScope`）與 `getCollectedMovieIds()`，標記分頁電影清單的 `isCollect`。
  - `GetMovieDetailUseCase`：呼叫 `MovieRepository.getMovieDetail(movieId)`，成功時額外寫入 `insertMovieHistory(...)`。
  - `GetMovieRecommendUseCase`：合併 `MovieRepository.getMovieRecommendations(movieId)` 與 `getCollectedMovieIds()`，標記推薦清單的 `isCollect`。
- IO 排程：延續 `data` 層已建立的 `common/di/CommonModule.kt` 的 `CommonDispatcher.IO` qualified `CoroutineDispatcher`，本次不修改 `CommonModule.kt`，5 個 UseCase 皆以必要建構子參數（無預設值）注入 `ioDispatcher`。
- 新增 Koin `domainModule()`，提供 5 個 UseCase 的 binding（`factory { GetXxxUseCase(get(), get(qualifier = named(CommonDispatcher.IO))) }` 等），依賴既有 `dataModule()` 已提供的 `MovieRepository`／`UserDataRepository`。UseCase 用 `factory` 而非 `single`（無內部可變狀態，比照一般 UseCase 慣例，每次注入取得新實例）。
- 擴充共用 `initKoin(...)` 進入點，安裝 `domainModule()`。
- 新增聚焦測試，涵蓋 5 個 UseCase 行為（透過 fake `MovieRepository`／`UserDataRepository`）與 Koin module 能否正確 resolve 全部 5 個 UseCase。

## Capabilities

### New Capabilities

- `kmp-movie-domain-usecases`：shared KMP 電影 UseCase 層，整合 `MovieRepository`／`UserDataRepository` 提供 5 個 UseCase（configuration 快取退回、瀏覽紀錄／首頁分頁清單／推薦清單標記收藏狀態、查看詳情寫入瀏覽紀錄），並透過 Koin `domainModule` 對外提供。

### Modified Capabilities

（無；沿用既有 `common-kernel` 的 `CommonDispatcher.IO`，不需要修改其需求）

## Impact

- **受影響 module**：`shared`、`androidApp`、`iosApp`
- **受影響 source sets**：
  - `shared/commonMain`：新增 `domain`（`usecase`、`di`）package；擴充 `initKoin(...)` 安裝 `domainModule()`
  - `shared/commonTest`：新增 5 個 UseCase 行為測試、`domainModule` resolve 測試、`InitKoinTest` 補上 UseCase resolve 驗證，新增 fake `MovieRepository`／`UserDataRepository`（`domain` 層依賴的是 Repository 介面本身，而非底層 `MovieDataSource`／DAO，既有 `commonTest/data` 底下的 fake 是針對 `MovieDataSource`／DAO，無法直接沿用）
- **不受影響**：`shared/androidMain`、`shared/iosMain`（`domainModule()` 沒有平台專屬邏輯，不需要 expect/actual）；`common/di/CommonModule.kt`、`data/repository/*`（本次不修改既有介面或行為）
- **不新增 Gradle 依賴**：`androidx.paging:paging-common`（`cachedIn` 擴充函式）已存在於 `shared/build.gradle.kts`；qualifier 沿用既有 Koin `named(CommonDispatcher.IO)` 機制
