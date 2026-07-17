## 為什麼

JetpackMovieCompose/core/common 定義了跨層共用的 `LanguageProvider`、`BaseHostUrlProvider`、`NetworkException` 等契約，讓 network 與 datastore 各自依賴介面而不互相耦合。目前 KMP 專案的對應型別散落在 `network` package 底下（`LanguageProvider` 介面、`NetworkException`），datastore 端要提供實作或處理錯誤時得反過來 import network 的型別；同時 KMP 專案完全沒有 `BaseHostUrlProvider` 對應概念——TMDB 圖片 CDN 的 base URL（來自 `/configuration` API、隨使用者持久化設定變動）目前無法被消費端取得。另外，`datastoreModule` 目前自行定義了一份 `CoroutineScope` binding，只給 `network/provider` 底下的 datastore-backed provider 使用，職責上比較適合抽成共用的 DI 提供者。

調查過程也發現：目前只有 `androidApp` 呼叫 `startKoin`，`iosApp` 完全沒有初始化 Koin，代表整個 network/datastore DI graph 在 iOS 上實際上還沒被啟動過。這次一併補上，讓這次新增的 `BaseHostUrlProvider` 等能力在兩個平台都能真正被解析。

這次變更會在 `shared/commonMain` 新增 `common` package，把 `LanguageProvider`、`NetworkException` 移過去、新增 `BaseHostUrlProvider` 介面，並新增一個提供共用 `CoroutineScope` 的 `commonModule()`；同時在 `shared/commonMain` 新增一個平台共用的 `initKoin()` 進入點，讓 `androidApp`、`iosApp` 都透過同一份邏輯啟動 Koin。

## 變更內容

- 在 `shared/src/commonMain/kotlin/com/shang/jetpackmoviekmp/` 下新增 `common` package（套件名 `com.shang.jetpackmoviekmp.common`）。
- 將 `network/provider/LanguageProvider` 介面搬到 `common`；`DefaultLanguageProvider`、`DatastoreLanguageProvider`、`networkModule`、`datastoreModule` 及對應測試改為 import 新位置，實作邏輯與所在位置不變。
- 將 `network/model/NetworkException.kt`（含 `toNetworkException()` extension）搬到 `common`；`network/model`、`network/datasource`、`network/extension` 內所有引用點改 import。
- 在 `common` package 新增 `BaseHostUrlProvider` 介面（`fun getBaseHostUrl(): String`）。
- 比照 `DatastoreLanguageProvider` 模式，在 `network/provider` 新增 `DatastoreBaseHostUrlProvider`：從 `UserPreferenceDataSource.userData.map { it.configuration.images.baseUrl }` 收集並快取最新值。
- 新增 `common` 的 Koin `commonModule()`，提供 `single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.Default) }`；`datastoreModule` 移除自己重複定義的 scope binding，改依賴 `commonModule()` 提供的 binding。
- `datastoreModule` 新增 `single<BaseHostUrlProvider> { DatastoreBaseHostUrlProvider(get(), get()) }` 綁定。
- 在 `shared/commonMain` 新增 `initKoin(dataStore, isDebug, appDeclaration)` 進入點，統一安裝 `commonModule()`、`datastoreModule(dataStore)`、`networkModule(isDebug, provideDefaultLanguageProvider = false)`。
- `androidApp/JetpackMovieApplication` 改呼叫共用的 `initKoin(...)`（透過 `appDeclaration` 傳入 `androidContext(this)`），取代原本直接呼叫 `startKoin { ... }` 的寫法。
- `iosApp/iOSApp.swift` 新增呼叫 `initKoin(...)`（透過 Kotlin/Native 匯出的 `Shared` framework），讓 Koin 在 iOS app 啟動時被初始化。

## Capabilities

### New Capabilities

- `common-kernel`：`shared/commonMain/common` 提供跨層共用的介面與型別（`LanguageProvider`、`BaseHostUrlProvider`、`NetworkException`）以及共用 `CoroutineScope` 的 Koin `commonModule()`，讓 network 與 datastore 各自依賴這一層而不直接耦合彼此的實作。
- `ios-koin-bootstrap`：`iosApp` 在啟動時初始化 Koin DI graph（透過 shared 的 `initKoin()` 進入點），讓 `commonModule`、`datastoreModule`、`networkModule` 在 iOS 上可被解析；`androidApp` 也改用同一個進入點，維持雙平台啟動邏輯一致。

### Modified Capabilities

- `kmp-user-preferences-datastore`：Koin `datastoreModule` 新增可解析 `BaseHostUrlProvider`（datastore-backed）的需求；`CoroutineScope` binding 改由 `commonModule` 提供，`datastoreModule` 需搭配 `commonModule` 安裝才能完整解析。

## Impact

- **受影響 module**：`shared`、`androidApp`、`iosApp`
- **受影響 source sets**：
  - `shared/commonMain`：新增 `common` package（`LanguageProvider`、`BaseHostUrlProvider`、`NetworkException`、`commonModule`）；`network/provider`（import 調整、新增 `DatastoreBaseHostUrlProvider`）、`network/model`／`network/datasource`／`network/extension`（`NetworkException` import 調整）、`datastore/di`（移除自訂 scope binding、新增 `BaseHostUrlProvider` binding）；新增 `initKoin()` 進入點
  - `shared/commonTest`：既有 `LanguageProvider`／`NetworkException`／`DatastoreLanguageProvider`／`DatastoreModule` 測試調整 import；新增 `DatastoreBaseHostUrlProvider`、`commonModule`、`initKoin` 相關測試
  - `androidApp`：`JetpackMovieApplication` 改用共用 `initKoin()`
  - `iosApp`：`iOSApp.swift` 新增 Koin 啟動呼叫
- **Dependencies**：不需新增外部依賴或調整 `libs.versions.toml`——`ConfigurationBean.images.baseUrl`、`UserPreferenceDataSource`、Koin、`kotlinx.coroutines` 均已存在

## 非目標

- 不接上 Coil `HostInterceptor` 等 UI 層圖片載入邏輯（`androidApp`／`iosApp` 目前沒有圖片顯示 UI，留待未來 change）
- 不在 `networkModule` 新增 `BaseHostUrlProvider` 的 default fallback binding（僅在 `datastoreModule` 提供，因為沒有 datastore 時本來就不需要圖片 host）
- 不變更 TMDB API host（`https://api.themoviedb.org/3/`）本身的處理方式，`BaseHostUrlProvider` 只處理圖片 CDN base URL
- 不引入 Koin qualifier／多重 dispatcher（例如參考專案的 `CommonDispatcher` enum），`commonModule` 這次只提供單一 `CoroutineScope`
- 不在這次 change 內驗證 iOS 實機／模擬器上的端對端行為（開發機為 Windows，無法執行 iOS simulator tests，僅能透過 compile 與 commonTest 覆蓋，詳見 design.md 風險章節）
