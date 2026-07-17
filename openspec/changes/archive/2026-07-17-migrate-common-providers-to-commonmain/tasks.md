## 1. 新增 common package：LanguageProvider / BaseHostUrlProvider

- [x] 1.1 建立 `shared/src/commonMain/kotlin/com/shang/jetpackmoviekmp/common/`。
- [x] 1.2 新增 `common/LanguageProvider.kt`（`fun getLanguageCode(): String`），內容從 `network/provider/LanguageProvider.kt` 搬移，套件改為 `com.shang.jetpackmoviekmp.common`。
- [x] 1.3 新增 `common/BaseHostUrlProvider.kt`（`fun getBaseHostUrl(): String`）。
- [x] 1.4 刪除舊的 `network/provider/LanguageProvider.kt`。

## 2. 遷移 NetworkException 到 common

- [x] 2.1 新增 `common/NetworkException.kt`，內容從 `network/model/NetworkException.kt` 搬移（含 `HttpError`/`ConnectionError`/`TimeoutError`/`ParseError`/`UnknownError` 與 `Throwable.toNetworkException()`），套件改為 `com.shang.jetpackmoviekmp.common`。
- [x] 2.2 刪除舊的 `network/model/NetworkException.kt`。
- [x] 2.3 更新 `network/model/NetworkResponse.kt`、`network/datasource/MovieDataSourceImpl.kt`、`network/extension/NetworkExtension.kt` 等引用點的 import。

## 3. 更新既有 LanguageProvider 實作與 DI 的 import

- [x] 3.1 更新 `network/provider/DefaultLanguageProvider.kt` 的 import，指向 `common.LanguageProvider`。
- [x] 3.2 更新 `network/provider/DatastoreLanguageProvider.kt` 的 import，指向 `common.LanguageProvider`。
- [x] 3.3 更新 `network/di/NetworkModule.kt` 的 import。
- [x] 3.4 更新 `datastore/di/DatastoreModule.kt` 的 import。

## 4. 新增 BaseHostUrlProvider 的 datastore-backed 實作

- [x] 4.1 新增 `network/provider/DatastoreBaseHostUrlProvider.kt`：建構子接收 `UserPreferenceDataSource`、`CoroutineScope`，`init` 中 collect `userData.map { it.configuration.images.baseUrl }.distinctUntilChanged()` 並快取到 `@Volatile` 欄位。
- [x] 4.2 實作尾端 `/` 正規化（非空字串補上 `/`，空字串維持空字串），對齊參考專案 `BaseHostUrlProviderImpl` 行為。
- [x] 4.3 `datastoreModule` 新增 `single<BaseHostUrlProvider> { DatastoreBaseHostUrlProvider(get(), get()) }`。

## 5. 抽出共用 CoroutineScope 為 commonModule

- [x] 5.1 新增 `common/di/CommonModule.kt`，定義 `fun commonModule() = module { single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.Default) } }`。
- [x] 5.2 `datastore/di/DatastoreModule.kt` 移除自帶的 `single<CoroutineScope>` binding。
- [x] 5.3 檢查所有安裝 `datastoreModule` 的地方（production、tests）一併安裝 `commonModule()`。

## 6. 新增 shared 的 initKoin 進入點

- [x] 6.1 在 `shared/commonMain` 新增 `initKoin(dataStore: DataStore<Preferences>, isDebug: Boolean, appDeclaration: KoinAppDeclaration = {})`，內部 `startKoin { appDeclaration(); modules(commonModule(), datastoreModule(dataStore), networkModule(isDebug, provideDefaultLanguageProvider = false)) }`。
- [x] 6.2 更新 `androidApp/JetpackMovieApplication.kt`，改呼叫 `initKoin(dataStore = createUserPreferencesDataStore(this), isDebug = isDebug) { androidContext(this@JetpackMovieApplication) }`，移除原本直接呼叫 `startKoin { ... }` 的邏輯。
- [x] 6.3 更新 `iosApp/iosApp/iOSApp.swift`，於 app 初始化時呼叫 shared 匯出的 `initKoin(...)`（Kotlin/Native 產生的 Swift API，必要時視 interop 情況新增 iOS-only wrapper，見 design.md 決策 4）。
- [x] 6.4 若 Swift 端無法直接使用帶 `appDeclaration` 預設值的簽名，於 `iosMain` 新增不需要 `appDeclaration` 參數的 wrapper 函式供 iOS 呼叫。

## 7. Tests

- [x] 7.1 更新既有測試（`DatastoreLanguageProviderTest`、`NetworkModuleTest`、`DatastoreModuleTest`、`DatastoreBackedLanguageRequestTest`、`MovieDataSourceImplTest` 等）的 import，指向新的 `common.LanguageProvider`／`common.NetworkException`，並補上 `commonModule()` 安裝，確認斷言內容不變。
- [x] 7.2 新增 `DatastoreBaseHostUrlProviderTest`：涵蓋預設值（尚未持久化 configuration 時為空字串）、值更新後快取同步反映、尾端 `/` 正規化三種情境。
- [x] 7.3 新增/更新 `DatastoreModuleTest`，確認 Koin 搭配 `commonModule()` 可 resolve `BaseHostUrlProvider` 且為 datastore-backed 實例；新增一則測試驗證缺少 `commonModule()` 時 resolve 會失敗。
- [x] 7.4 新增 `CommonModuleTest`，確認 `commonModule()` 可單獨 resolve `CoroutineScope`。
- [x] 7.5 新增 `initKoin` 的 Koin test：呼叫 `initKoin(...)` 後可完整 resolve `MovieDataSource`、`UserPreferenceDataSource`、`LanguageProvider`、`BaseHostUrlProvider`。
- [x] 7.6 新增 `common` package 的 import 邊界檢查（可用簡單靜態檢查或於 code review 中人工確認 `common` 不 import `network`／`datastore`）。

## 8. Verification

- [x] 8.1 執行 `.\gradlew.bat :shared:testAndroidHostTest`，確認所有既有與新增測試通過。（BUILD SUCCESSFUL）
- [x] 8.2 執行 `.\gradlew.bat :androidApp:assembleDebug`，確認 import 調整與 `initKoin` 改動後仍可正常編譯執行。（BUILD SUCCESSFUL）
- [x] 8.3 執行 `ktlintCheck`，確認新增/搬移的檔案符合專案格式規範。（BUILD SUCCESSFUL）
- [x] 8.4 確認 iOS 端（`shared` 的 iosMain/iosSimulatorArm64Test 及 `iosApp` Swift 程式碼）可以編譯；記錄此開發機是否能實際執行 iOS simulator 驗證。（`:shared:compileKotlinIosSimulatorArm64`、`:shared:compileTestKotlinIosSimulatorArm64` 皆 BUILD SUCCESSFUL，代表 iosMain/iosTest 的 Kotlin 原始碼含新增的 `InitKoinIos.kt` 可正確編譯成 klib；`iosApp/iosApp/iOSApp.swift` 因需要 Xcode/macOS 工具鏈，此 Windows 開發機無法編譯或執行驗證，見 8.5）
- [x] 8.5 記錄任何環境限制（例如 Windows 主機無法執行 iOS simulator tests，Swift `initKoin` 呼叫僅完成 compile-level 驗證）。（見下方「環境限制紀錄」）

### 環境限制紀錄

- **iOS simulator tests**：Windows 主機無法執行 `iosSimulatorArm64Test`，Gradle 自動略過
  （`Task 'iosSimulatorArm64Test' for target 'ios_simulator_arm64' cannot run on the current host (windows-x86_64)`）。
- **Swift 端 `iOSApp.swift` 呼叫 `InitKoinIosKt.doInitKoinIos(isDebug:)`**：僅完成 Kotlin 側
  （`InitKoinIos.kt`）的 klib 編譯驗證（`compileKotlinIosSimulatorArm64` BUILD SUCCESSFUL），
  Swift 程式碼本身未經 Xcode 編譯，也未經模擬器/實機驗證。刻意設計成不需要 `appDeclaration`
  參數、不需要 Swift 端自行組出 `DataStore` 的簡化版本（見 design.md 決策 4），降低了 interop 風險，
  但仍待有 macOS/Xcode 環境時實際編譯執行 `iosApp` 確認。
- **Android 實機/模擬器驗證**：本次僅執行 `testAndroidHostTest`（JVM 上執行的單元測試）與
  `assembleDebug`（編譯打包），未安裝到實機或模擬器上做端對端驗證。
