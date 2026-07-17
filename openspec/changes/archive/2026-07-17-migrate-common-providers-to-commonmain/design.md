## Context

現況：`shared/commonMain` 已有 `network/provider/LanguageProvider`（含 `DefaultLanguageProvider`、`DatastoreLanguageProvider`）、`network/model/NetworkException`與 `datastore` package（`UserPreferenceDataSource`、`datastoreModule`），已透過先前的 `migrate-datastore-to-commonmain` change 完成並測試。`datastoreModule` 目前自行定義 `single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.Default) }`，只給 `network/provider` 底下的 `DatastoreLanguageProvider` 使用。目前只有 `androidApp/JetpackMovieApplication` 呼叫 `startKoin`；`iosApp/iOSApp.swift` 是空的 SwiftUI App，完全沒有初始化 Koin。

JetpackMovieCompose 舊專案的 `core/common` 提供 `LanguageProvider`、`BaseHostUrlProvider`、`NetworkException`、`CommonDispatcher`/`ApplicationScope`（Hilt qualifier）等，實作分別放在 `core/datastore`（`BaseHostUrlProviderImpl` 讀取 `configuration.images.baseUrl`）與消費端（`core/ui` 的 `HostInterceptor`，用於 Coil 圖片請求補上 host 前綴）。

這次要把「介面/型別定義」與「具體實作、DI wiring」的依賴方向理清，並補上兩個平台一致的 DI 啟動路徑。

## Goals / Non-Goals

**Goals:**

- `LanguageProvider`、`NetworkException` 搬到中立的 `commonMain/common` package，network 與 datastore 都改依賴新位置，行為與既有測試全部維持不變。
- 新增 `BaseHostUrlProvider` 介面於 `common`，並提供 datastore-backed 實作。
- 把 `CoroutineScope` 的提供責任從 `datastoreModule` 抽到獨立的 `commonModule()`，讓 `datastoreModule`、未來其他需要 scope 的 module 都能共用同一份。
- 新增 `shared/commonMain` 的 `initKoin()` 進入點，讓 `androidApp`、`iosApp` 使用同一份模組組裝邏輯啟動 Koin；補上 `iosApp` 目前缺少的 `startKoin` 呼叫。

**Non-Goals:**

- 不搬移或重寫 `DefaultLanguageProvider`、`DatastoreLanguageProvider` 的實作邏輯（只搬介面、改 import）。
- 不建立獨立 Gradle module 承載 `common` package。
- 不遷移 `UiState`、`CommonDispatcher`（`CommonDispatcher` 是 Hilt-specific qualifier 概念，KMP 專案已用 Koin，沒有直接對應必要；這次 `commonModule` 只提供單一未命名的 `CoroutineScope`，不引入 dispatcher qualifier 體系）。
- 不接上 Coil `HostInterceptor` 或任何 UI 層圖片載入邏輯。
- 不在這次 change 驗證 iOS 實機／模擬器端對端行為。

## Decisions

### 1. `common` package 定位調整為「跨層共用型別」，不再侷限於純介面

原本規劃 `common` 只放 provider 介面。這次追加 `NetworkException` 之後，`common` 的定位調整為：放跨層共用、不依賴 `network`／`datastore` 具體實作的型別（介面 + 資料/例外型別），但**不**放任何有副作用或平台相依的邏輯。`common` package 本身仍然 MUST NOT import `network` 或 `datastore` 底下的型別——依賴方向永遠是 `network`／`datastore` → `common`。

`commonModule()`（Koin module function）例外於這個純型別限制，因為它是 DI wiring 而非資料型別，但其內容只提供 `CoroutineScope`，不 import 任何 `network`／`datastore` 型別，所以依然維持 `common` 不反向依賴的原則。

替代方案：把 `NetworkException` 留在 `network/model`，只在 `common` 放 provider 介面。討論後不採用，因為既然要建立「跨層共用型別」的落腳點，讓 `NetworkException`（未來 UI 層、其他 feature module 都可能需要引用）跟 provider 介面放在一起，比分散在 `network` 底下更一致；且 KMP 專案自己的 `NetworkException` 已經是 KMP-safe（依賴 Ktor multiplatform exception 型別，不依賴 `java.net.*`），沒有相容性障礙。

### 2. `BaseHostUrlProvider` 比照 `DatastoreLanguageProvider` 的快取 + collect 模式

`DatastoreBaseHostUrlProvider` 建構子接收 `UserPreferenceDataSource` 與 `CoroutineScope`，`init` 中 collect `userData.map { it.configuration.images.baseUrl }.distinctUntilChanged()`，快取進 `@Volatile` 欄位，`getBaseHostUrl()` 同步回傳快取值，並比照參考專案補上尾端 `/` 正規化。實作仍留在 `network/provider`（與 `DatastoreLanguageProvider` 相同位置），只有介面搬到 `common`，維持範圍最小、不影響既有已通過測試的程式碼路徑。

### 3. `CoroutineScope` 抽成獨立 `commonModule()`

現況 `datastoreModule` 自己定義的 `single<CoroutineScope>` 其實跟「datastore 持久化」這個職責無關，純粹是給 `network/provider` 底下需要背景收集 flow 的 provider（`DatastoreLanguageProvider`、`DatastoreBaseHostUrlProvider`）用的共用資源。抽成 `commonModule()` 後：

```kotlin
fun commonModule() = module {
    single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
}
```

`datastoreModule` 不再自帶這個 binding，改為依賴呼叫端一併安裝 `commonModule()`（見決策 4 的 `initKoin()`）。

替代方案：維持現狀，`CoroutineScope` 繼續放在 `datastoreModule`。不採用，因為語意上這個 scope 的生命週期是「整個 app process」，跟 datastore 的持久化職責無關，未來如果有其他非 datastore 的 provider 也需要背景 scope，硬塞進 `datastoreModule` 會讓職責混亂。

### 4. 新增 `shared/commonMain` 的 `initKoin()`，統一雙平台啟動邏輯

```kotlin
fun initKoin(
    dataStore: DataStore<Preferences>,
    isDebug: Boolean,
    appDeclaration: KoinAppDeclaration = {},
) {
    startKoin {
        appDeclaration()
        modules(
            commonModule(),
            datastoreModule(dataStore),
            networkModule(isDebug = isDebug, provideDefaultLanguageProvider = false),
        )
    }
}
```

`androidApp/JetpackMovieApplication` 改成：

```kotlin
initKoin(
    dataStore = createUserPreferencesDataStore(this),
    isDebug = isDebug,
) { androidContext(this@JetpackMovieApplication) }
```

`iosApp/iOSApp.swift`（透過 `import Shared` 匯出的 Kotlin/Native framework）在 `init()` 呼叫：

```swift
InitKoinKt.doInitKoin(dataStore: createUserPreferencesDataStore(), isDebug: false)
```

（實際函式簽名與 Swift 端呼叫方式在實作階段依 Kotlin/Native 產生的 Objective-C header 確認；`isDebug` 在 iOS 端先固定為 `false`，日後若需要區分 debug/release，再比照 Android 的 `ApplicationInfo.FLAG_DEBUGGABLE` 引入等效機制。）

`createUserPreferencesDataStore()` 目前是兩個平台各自的頂層函式（不是嚴格的 `expect`/`actual`），Android 版本需要 `Context`、iOS 版本不需要，這次維持這個既有結構，`initKoin()` 只負責接收已經建好的 `DataStore`，不負責建立它。

替代方案：用 `expect fun platformInitKoin()` 把平台差異藏在 `expect`/`actual` 裡，`initKoin()` 完全不需要呼叫端傳入 `appDeclaration`／`dataStore`。不採用，因為現有 `createUserPreferencesDataStore` 已經是平台各自呼叫的模式，改成 `expect`/`actual` 是這次範圍外的既有程式碼重構，維持呼叫端組裝 `DataStore`、`initKoin()` 只負責 module 安裝，改動面最小。

## Risks / Trade-offs

- **[Risk]** 搬移 `LanguageProvider`、`NetworkException` 介面/型別會動到多個既有檔案（`DefaultLanguageProvider`、`DatastoreLanguageProvider`、`NetworkModule`、`DatastoreModule`、`NetworkResponse`、`MovieDataSourceImpl`、`NetworkExtension` 及對應測試）的 import，範圍看似小但觸點多 → **[Mitigation]** 全部改完後執行 `:shared:testAndroidHostTest`，確認既有測試全數通過，且只調整 import，不修改任何既有斷言。
- **[Risk]** 抽出 `commonModule()` 後，任何忘記一併安裝 `commonModule()` 的呼叫端（例如舊測試檔案）會讓 `DatastoreLanguageProvider`／`DatastoreBaseHostUrlProvider` 解析失敗（`CoroutineScope` 找不到 binding）→ **[Mitigation]** 統一透過 `initKoin()` 進入點安裝三個 module；commonTest 中原本各自 `startKoin { modules(datastoreModule(...), networkModule(...)) }` 的地方要一併補上 `commonModule()`，並新增測試明確驗證「只裝 `datastoreModule` 不裝 `commonModule`」時會 resolve 失敗，避免這個耦合被靜默忽略。
- **[Risk]** `DatastoreBaseHostUrlProvider` 目前沒有任何消費端（尚無圖片 UI）→ **[Mitigation]** proposal 非目標已明確說明這是為未來圖片載入鋪路的基礎設施，並在 tasks 中安排測試覆蓋其行為。
- **[Risk]** iOS 端新增 `startKoin` 呼叫無法在這台 Windows 開發機上跑 simulator 驗證端對端行為 → **[Mitigation]** 透過 `shared` 的 `iosMain`/`iosSimulatorArm64Test` compile 與既有 commonTest 覆蓋邏輯正確性；Swift 端呼叫程式碼在 tasks 中列為需要有 iOS 環境時人工驗證的項目，並在 verification 段落記錄限制。
- **[Risk]** Swift 對 Kotlin `KoinAppDeclaration`（一個帶預設值的 lambda 型別參數）的互操作性需要在實作階段確認產生的 Objective-C header 是否可直接呼叫、或需要額外包一層無 lambda 參數的 iOS-only 函式 → **[Mitigation]** 若直接呼叫有 interop 問題，改為在 `iosMain` 新增一個不帶 `appDeclaration` 參數的 `actual`/wrapper 函式供 Swift 呼叫，`common` 的 `initKoin()` 簽名不變。

## Migration Plan

1. 在 `shared/commonMain` 新增 `common` package：搬移 `LanguageProvider.kt`、`NetworkException.kt`，新增 `BaseHostUrlProvider.kt`。
2. 更新 `network/provider/DefaultLanguageProvider.kt`、`DatastoreLanguageProvider.kt`、`network/model/NetworkResponse.kt`、`network/datasource/*`、`network/extension/NetworkExtension.kt` 的 import。
3. 更新 `NetworkModule.kt`、`DatastoreModule.kt` 的 import；`DatastoreModule.kt` 移除自訂 `CoroutineScope` binding。
4. 新增 `common/di/CommonModule.kt`（`commonModule()`，提供 `CoroutineScope`）。
5. 新增 `network/provider/DatastoreBaseHostUrlProvider.kt`；`datastoreModule` 新增其 binding。
6. 新增 `shared/commonMain` 的 `initKoin()` 進入點。
7. 更新 `androidApp/JetpackMovieApplication`，改呼叫 `initKoin(...)`。
8. 新增 `iosApp/iOSApp.swift` 的 Koin 啟動呼叫。
9. 更新既有測試 import 與 `startKoin` 呼叫（補上 `commonModule()`）；新增新增型別/模組的對應測試。
10. 執行 `:shared:testAndroidHostTest`、`:androidApp:assembleDebug`、iOS compile 驗證。

沒有需要 rollback 的執行期狀態（無 DB schema、無 remote migration），如需回退直接 revert commit 即可。

## Open Questions

- 若之後要接 Coil 圖片載入，等同 `HostInterceptor` 的邏輯要放在 `shared`（KMP 共用）還是各平台 UI 層？留待該次 change 決定。
- `BaseHostUrlProvider` 是否需要跟 `LanguageProvider` 一樣，針對「尚未持久化任何值」的情境提供更明確的預設值？目前沿用參考專案的「空字串」行為。
- iOS 的 `isDebug` 判斷這次先寫死 `false`，之後是否要接上 Xcode build configuration 或其他機制反映 debug/release，留待需要時再處理。
