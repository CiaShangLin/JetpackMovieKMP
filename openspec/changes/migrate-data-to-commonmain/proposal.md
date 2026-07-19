## Why

`JetpackMovieCompose/core/data` 是參考專案的 Repository 層，整合 `core/network`（`MovieDataSource`）、`core/database`（`MovieCollectDao`／`MovieHistoryDao`）、`core/datastore`（`UserPreferenceDataSource`），對外提供 `MovieRepository`／`UserDataRepository` 兩個介面，並用 Hilt `@Binds` 綁定實作。目前 KMP 專案的 `shared/commonMain` 已經個別遷移完成 `network`（`ktor-movie-network`）、`database`（`kmp-movie-local-database`）、`datastore`（`kmp-user-preferences-datastore`）三層，但彼此仍是獨立元件，沒有任何整合層對外提供單一電影資料存取 API——UI 層若要拿到電影分頁列表、收藏／瀏覽紀錄、使用者偏好設定，必須自行組合三個底層依賴，重複參考專案已經寫好的整合邏輯（Paging 分頁、collect/history CRUD、configuration 持久化）。

這次變更會把 `core/data` 遷移到 `shared/src/commonMain/kotlin/com/shang/jetpackmoviekmp/data`，沿用先前 `datastore`／`database`／`common providers` 遷移的既有模式：commonMain 定義 Repository 介面與實作、Paging 3 KMP（`androidx.paging:paging-common`，`shared/build.gradle.kts` 已具備此依賴）分頁邏輯、Koin module 對外提供綁定，並比照既有 `initKoin` 串接方式安裝新 module。這次僅遷移到 `commonMain`，不建立獨立的 Gradle module（例如 `core:data`）；Module 化留待之後的 change 處理。

## What Changes

- 在 `shared/src/commonMain/kotlin/com/shang/jetpackmoviekmp/` 下新增 `data` package，包含 `repository`、`paging`、`model`（mapper）、`di` 子 package。
- 將參考專案的 Repository 結構遷移到 KMP：
  - `MovieRepository`／`MovieRepositoryImpl`：整合 `MovieDataSource`（configuration、genres、movie detail、recommendations、cast/crew）、`MovieCollectDao`／`MovieHistoryDao`（收藏與瀏覽紀錄的新增/刪除/查詢）、`Pager`（電影列表與搜尋分頁）。
  - `UserDataRepository`／`UserDataRepositoryImpl`：包裝既有 `UserPreferenceDataSource`，提供 `userData: Flow<UserData>`、`setConfiguration`／`setThemeMode`／`setLanguageMode`。
  - `MovieGenrePagingSource`／`MovieSearchPagingSource`：沿用參考專案的 `PagingSource<Int, MovieCardResult>` 實作，改用 commonMain 的 `MovieDataSource`／`NetworkResponse`。
  - `MovieMapper`：`MovieCardResult` 與 `MovieCollectEntity`／`MovieHistoryEntity` 之間的雙向轉換（`asCollectEntity()`／`asHistoryEntity()`；entity → model 的 `asExtendedModel()` 已存在於 `database.entity`，直接沿用不重複定義）。
- IO 排程：參考專案透過 Hilt `@Dispatcher(CommonDispatcher.IO)` 注入 `CoroutineDispatcher`。本次擴充 `common/di/CommonModule.kt`：新增 `enum class CommonDispatcher { IO }`，並在 `commonModule()` 新增帶 qualifier 的 `single<CoroutineDispatcher>(qualifier = named(CommonDispatcher.IO)) { Dispatchers.IO }`，比照既有集中提供 `CoroutineScope` 的模式，供 `data` 層與未來其他 module 共用（**MODIFIED** `common-kernel` capability）。`MovieRepositoryImpl` 以必要建構子參數（無預設值）注入這個 `ioDispatcher`，正式環境由 `dataModule()` 透過 `get(qualifier = named(CommonDispatcher.IO))` 取得，測試則直接建構、傳入 test dispatcher，不需要透過 Koin。
- 新增 Koin `dataModule()`，提供 `MovieRepository`、`UserDataRepository` 的 binding（`single<MovieRepository> { MovieRepositoryImpl(get(), get(), get(), get(qualifier = named(CommonDispatcher.IO))) }` 等），依賴既有 `networkModule`／`databaseModule`／`datastoreModule`／擴充後 `commonModule` 已提供的 `MovieDataSource`／DAO／`UserPreferenceDataSource`／`CoroutineDispatcher`。
- 擴充共用 `initKoin(...)` 進入點，安裝 `dataModule()`（不需要新增任何平台專屬參數，因為 `dataModule()` 依賴的元件皆已由既有 module 提供）。
- 新增聚焦測試，涵蓋 `MovieRepositoryImpl`／`UserDataRepositoryImpl` 行為（透過假的 `MovieDataSource`／DAO／`UserPreferenceDataSource` 或既有 test double）與 Koin module 能否正確 resolve `MovieRepository`／`UserDataRepository`。

## Capabilities

### New Capabilities

- `kmp-movie-data-repository`：shared KMP 電影資料整合層，聚合 network／database／datastore 提供 `MovieRepository`（電影列表與搜尋分頁、電影詳情、收藏與瀏覽紀錄）與 `UserDataRepository`（使用者偏好設定），並透過 Koin `dataModule` 對外提供。

### Modified Capabilities

- `common-kernel`：`commonModule()` 新增帶 qualifier 的 `CoroutineDispatcher` single（`CommonDispatcher.IO`），供 `data` 層與未來其他 module 透過 `named(CommonDispatcher.IO)` 取得，不改動既有 `CoroutineScope`／provider 相關需求。

## Impact

- **受影響 module**：`shared`、`androidApp`、`iosApp`
- **受影響 source sets**：
  - `shared/commonMain`：新增 `data`（`repository`、`paging`、`model`、`di`）package；擴充 `common/di/CommonModule.kt`（新增 `CommonDispatcher` enum 與 `CoroutineDispatcher` qualified single）；擴充 `initKoin(...)` 安裝 `dataModule()`
  - `shared/commonTest`：`MovieRepositoryImpl`／`UserDataRepositoryImpl` 行為測試、`dataModule` resolve 測試、mapper 測試、`CommonModuleTest` 補上 `CoroutineDispatcher` resolve 驗證
- **不受影響**：`shared/androidMain`、`shared/iosMain`（`dataModule()`／擴充後的 `commonModule()` 都沒有平台專屬邏輯，不需要 expect/actual）
- **不新增 Gradle 依賴**：`androidx.paging:paging-common` 已存在於 `shared/build.gradle.kts`；qualifier 用 Koin 既有 plain DSL 的 `named(...)`，不需要 Koin Annotations（`koin-ksp-compiler`）
