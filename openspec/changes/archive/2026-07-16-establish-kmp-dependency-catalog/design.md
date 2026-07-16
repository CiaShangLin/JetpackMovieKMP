## Context

來源專案 `JetpackMovieCompose` 有 15 個 app/core/feature 模組，以 `buildSrc` 的 `DependenciesVersions`、`Dependencies`、`DependenciesProvider` 和 `SharedLibraryGradlePlugin` 同時管理依賴與 Android convention。`androidx()` helper 將 Compose、Navigation、Paging、Coil、WorkManager、Gson 與 Android UI 套件一起加入所有模組，無法直接映射到 KMP source sets。

目標專案已採 AGP 9 的 Android-KMP plugin、Kotlin 2.4.0、Compose Multiplatform 1.11.1、Android/iOS targets 與 Version Catalog。本 change 先建立依賴契約和最小編譯驗證，不搬移舊功能。版本基準日為 2026-07-16，正式實作前仍須以官方 release notes 再確認一次相容性。

## Goals / Non-Goals

**Goals:**

- 讓所有外部 plugin 與 library 版本集中於 `libs.versions.toml`，並能由 alias 看出用途。
- 將舊依賴分類為 common、Android、iOS、測試、替代或延後導入。
- 建立 Ktor、Koin、Room 2.8.4、SQLiteDriver 與 Kotlinx Serialization 的 catalog 契約與 source set 配置規則。
- 保留 CMP 作為可撤換的選用 UI layer，同時確保共用業務與資料模組維持純 KMP。
- 保持目前 MVVM、Repository、Use Case 分層方向；這次只替換基礎設施，不改成 MVI，也不改 domain contract。
- 讓現有 Android 與 iOS 主要編譯 task 能驗證 catalog accessor 與 plugin alias 配置。

**Non-Goals:**

- 不搬移 Retrofit service、OkHttp interceptor、Hilt module、Room DAO/entity 或 feature UI。
- 不在本 change 建立所有舊模組、convention plugin、flavor、簽章或 release pipeline。
- 不在本 change 決定最終 iOS UI 採 SwiftUI 或 CMP，也不新增或搬移任何共用畫面。
- 不承諾舊 Android app 的資料檔、package name 或安裝狀態可原地升級。
- 不預先加入尚未有遷移程式碼使用的 Android-only 套件。

## Decisions

### 1. Version Catalog 只管理外部座標與版本

採用 `[versions]`、`[libraries]`、`[plugins]`，alias 使用 kebab-case 並以生態系前綴分組，例如 `ktor-client-core`、`koin-compose-viewmodel`、`room-runtime`。專案模組依賴繼續使用 type-safe project accessors，不建立 module alias，也不把 namespace、SDK、flavor 或簽章硬塞進 TOML。

替代方案是複製 `buildSrc` helper；否決原因是它把依賴集合與 Android Gradle DSL 綁定，而且每個 KMP source set 需要更細的可見性。

### 2. 採穩定版優先的版本基線

| 類別 | 建議版本 | 處置 |
|---|---:|---|
| Gradle wrapper | 9.5.0 | AGP 9.3.0 最低需求 |
| AGP / Android-KMP plugin | 9.3.0 | 最新穩定版，支援 compileSdk 37 |
| Kotlin / Compose Compiler | 2.4.0 | 保留新專案現值，兩個 plugin 同版 |
| Compose Multiplatform | 1.11.1 | 暫時保留，僅供 optional CMP UI layer 使用 |
| KSP | 2.3.10 | 新增，僅套用到需要產生碼的模組 |
| Kotlin Coroutines | 1.11.0 | 取代舊版 Android-only `coroutines-android` 1.3.9，以 `core`/`test` aliases 分流 |
| Kotlinx Serialization JSON | 1.11.0 | 取代舊版 1.6.2 與 Gson converter |
| Ktor | 3.5.1 | 新增 client core、content negotiation、JSON、logging、mock 與平台 engines |
| Koin | 4.2.2 | 新增 core、Compose、ViewModel、test；先採 DSL，不採 RC compiler plugin |
| Room | 2.8.4 | 目前官方可解析穩定版，新增 runtime、compiler、paging 與 `androidx.room` plugin；Room 3 穩定發布後再另立 change |
| AndroidX SQLite | 2.7.0 | 新增 bundled driver，Android/iOS 使用一致 SQLite |
| Lifecycle | 2.11.0 | 最新穩定版；需搭配 AGP 9.1.0+ 與 compileSdk 37 |
| DataStore | 1.2.1 | 由 1.1.1 升級；共用格式另由 migration change 決定 |
| Paging | 3.5.0 | 由 3.3.6 升級，使用 KMP artifacts |
| Coil | 3.5.0 | 由 3.2.0 升級，網路層改用 `coil-network-ktor3` |
| Navigation 3 | 1.1.4 | Android Jetpack Compose 或 optional CMP UI 使用；SwiftUI 導航不依賴此 alias |
| WorkManager | 2.11.2 | 僅在確有背景工作遷移時加入 `androidApp`/`androidMain` |

版本來源以 Kotlin、JetBrains、Android Developers、Ktor、Koin 與 Coil 官方文件為準。Catalog 不使用 dynamic version、`+` 或任意 snapshot。

### 3. Ktor 取代 Retrofit 與 OkHttp

`commonMain` 使用 `ktor-client-core`、`ktor-client-content-negotiation`、`ktor-serialization-kotlinx-json` 和必要的 logging；`androidMain` 使用 CIO，`iosMain` 使用 Darwin，測試使用 MockEngine。TMDB API key 仍由未進版控的 `key.properties` 提供，日後在 `core/network` 的 Ktor request pipeline 實作 `ApiKeyInterceptor` 等價邏輯；語言偏好亦以 request plugin/default request 實作。

不加入 `ktor-client-okhttp`，也不以 Coil 的 `coil-network-okhttp` 間接帶回 OkHttp。Coil 必須使用 `coil-network-ktor3` 和既有平台 engine。

### 4. Koin DSL 取代 Hilt code generation

`commonMain` 使用 `koin-core`、`koin-compose`、`koin-compose-viewmodel`，平台物件透過平台 module 注入。採 constructor DSL/module verification，移除 Hilt plugin、compiler、`javax.inject` 與 Android annotations。

Koin 4.2 官方雖推薦 BOM 和 compiler plugin，但 compiler plugin 仍是 RC；本階段使用單一 `koin` version ref 與 DSL，避免在 Room 已需要 KSP 時再引入另一個預覽編譯器變數。後續可獨立評估 compiler plugin。

### 5. Room 以獨立 database 模組導入

Room 使用官方可解析的 `androidx.room:*:2.8.4` 座標與 `androidx.room` plugin，KSP compiler 必須對 Android、iOS device 與 simulator target 分別配置。Runtime 與 `room-paging` 放在共用 database 模組，SQLite 使用 `androidx.sqlite:sqlite-bundled:2.7.0`。

Room 3 目前尚未有穩定可解析座標，因此本階段不切換到 `androidx.room3` package。舊 DAO 若回傳 `PagingSource`，需加入 `room-paging`。直接低階操作改用 SQLiteDriver 的遷移保留在後續 database change。

### 6. 不把 Android-only 依賴放入 commonMain

Activity、AppCompat、Core KTX、SplashScreen、WorkManager、Chucker、Lottie、Android Material、JUnit/Espresso/MockK Android 僅可存在 `androidApp`、`androidMain` 或 Android test source set。舊專案沒有實際使用的 Sandwich 不導入；Gson、Retrofit adapter、OkHttp、Hilt 完全排除。

DataStore 可在 KMP 使用，但舊 `protobuf-javalite`/`protobuf-kotlin-lite` 不是直接可搬的 commonMain 契約，因此第一階段只建立 DataStore core alias，不加入 Protobuf plugin。後續遷移設定資料時，優先評估 Kotlinx Serialization serializer；因新 app 使用不同 applicationId，沒有強制沿用舊 on-device protobuf 的相容需求。

### 6.1 KMP placement analysis

| Library family | KMP placement | Notes |
|---|---|---|
| Kotlinx Coroutines | `commonMain` / `commonTest` | `coroutines-android` 只限 Android source set |
| Kotlinx Serialization | `commonMain` | 與 Ktor JSON pipeline 共用 |
| Ktor | `commonMain` core/features, `androidMain` CIO, `iosMain` Darwin | 不引入 OkHttp engine |
| Koin | `commonMain` core/test；Compose/ViewModel aliases 只給 UI layer | 不引入 Hilt 或 Koin compiler plugin |
| Room | database KMP module；KSP 依 target 配置 | 本階段採 `androidx.room` 2.8.4，Room 3 另案 |
| SQLite bundled | database KMP module | 與 Room KMP database 一起使用 |
| DataStore | `commonMain` | Protobuf plugin/JVM protobuf aliases 不預設導入 |
| Paging | `paging-common` 可放 common；runtime/compose 只放平台或 UI layer | 避免純 domain/usecase 依賴 UI paging adapter |
| Coil 3 | KMP image layer；`coil-compose` 只放 Compose UI layer | Network 使用 `coil-network-ktor3`，不使用 OkHttp |
| Lifecycle | Compose/ViewModel aliases 只放 UI layer | 純 KMP core 使用 Flow/StateFlow/suspend API |
| Navigation 3 | Android/CMP UI layer | 不放入純 KMP core |
| WorkManager, Activity, AppCompat, Core KTX, Espresso | Android-only | 不進 common source set |

### 7. 測試依賴按平台分層

`commonTest` 使用 `kotlin-test`、`kotlinx-coroutines-test`、`ktor-client-mock`、`koin-test`。JUnit 4/5、Espresso 與 Android Compose UI test 只保留在 Android 測試；MockK/Strikt 不先放進 commonTest，待確認 Native 支援與實際測試需求後再決定。測試採 AAA，遷移後的核心邏輯要求至少 80% 單元測試覆蓋率。

### 8. CMP 暫時保留但與純 KMP 核心隔離

目前保留 `org.jetbrains.compose`、Compose Compiler、Compose Multiplatform libraries 與 resources aliases，避免在尚未完成 iOS UI 試作前過早關閉 CMP 路線。保留 CMP 不代表採用 shared UI：第一階段不新增 CMP 畫面，Android 可使用 Jetpack Compose，iOS 可用 SwiftUI。

未來 `core:model`、`core:domain`、`core:data`、`core:network`、`core:database`、`core:datastore` 與 `core:common` 必須是純 KMP modules，不得套用 `org.jetbrains.compose`，公開 API 不得暴露 `@Composable`、Compose `State`、Painter、Modifier 或 Compose resources。跨平台狀態以 immutable model、`Flow`、`StateFlow` 與 suspend API 表達。

現有 `shared` 可暫時保留 CMP 範本內容，但不得繼續承擔新業務邏輯與新 UI。後續逐步把邏輯移入純 KMP core modules；若保留 CMP UI，應將它明確定位或改名為 `shared-ui`/`composeApp`。完成至少一個 SwiftUI vertical slice 後，再比較開發成本、平台體驗與維護負擔，決定保留或移除 CMP。

替代方案是現在立即移除 CMP；否決原因是使用者仍在評估 SwiftUI，保留隔離良好的 optional layer 成本可控且可逆。另一個替代方案是讓 core modules 直接依賴 Compose Runtime；否決原因是它會把 UI framework 滲入業務契約，使 SwiftUI 消費與未來移除 CMP 變困難。

## Risks / Trade-offs

- [新專案目前版本非常新，外部套件可能尚未完整宣告 Kotlin 2.4 metadata 相容性] -> 先以最小 source set 建置驗證；若失敗，只降單一受影響套件並在 TOML 註明原因，不回退整套工具鏈。
- [Room 3 尚未穩定發布，提前使用不可解析座標會阻斷 Gradle build] -> 本階段採用 `androidx.room` 2.8.4 穩定版，將 Room 3 package/API 遷移延後到獨立 change；仍禁止 `fallbackToDestructiveMigration` 作為正式策略。
- [Room schema migration 可能破壞既有 Android 資料] -> 新 applicationId 預設視為全新資料庫；若日後決定沿用舊 applicationId，必須另立 change 驗證 v1 schema、migration path 與真實資料庫檔。
- [Ktor engine 的 Android/iOS 行為與憑證、timeout、logging 不完全一致] -> 共用 client configuration，平台 engine 只處理 transport，並以 MockEngine 和平台 smoke test 驗證。
- [直接採最新穩定版會增加一次遷移的 API 差距] -> Catalog 先落地並逐群導入；每群都有編譯 checkpoint，避免一次修改全部模組。
- [預先加入所有舊依賴會造成未使用依賴與平台污染] -> TOML 只納入已確認的 KMP 基線；Android-only 套件採需求驅動新增。
- [保留 CMP 容易讓新程式繼續寫進 shared] -> 以 module plugin、dependency 與公開 API 規則阻止 Compose 進入純 KMP core，並將 shared 標記為 optional UI layer。
- [Android Jetpack Compose 與 iOS SwiftUI 需要維護兩套 UI] -> 先以單一功能完成 vertical slice，再決定原生 UI 的學習與維護成本是否可接受。

## Migration Plan

1. 建立 catalog aliases 與 root `apply false` plugin aliases，保留目前可建置狀態。
2. 只將既有 `shared/androidApp` 已使用的硬編依賴改為 catalog alias；新的 Ktor、Koin、Room 等 aliases 先不掛入 runtime。
3. 執行 catalog accessor、Android compile、iOS metadata/framework compile 與現有 common tests。
4. 後續依 `model/common -> network/database/datastore -> data/domain -> platform UI` 次序逐 change 導入 aliases 並遷移程式碼；CMP UI 不作為核心模組依賴。

回滾方式是移除本 change 新增的 aliases/plugin declarations/source set references；舊專案完全不修改，因此仍可獨立建置與發布。

## Open Questions

- DataStore 的 typed payload 最終採 Kotlinx Serialization、自訂 serializer，或保留平台限定 Protobuf，留待 datastore migration change 決定。
- Android 的 Navigation 3 與 iOS 的 SwiftUI NavigationStack 如何對應共用 route model，留待第一個跨平台 feature vertical slice 決定。
- CMP 的最終去留在完成至少一個 SwiftUI 畫面與 iOS Simulator 流程後決定。
- Chucker 沒有 KMP client 攔截能力；是否需要 Android-only network inspector，應在 `core/network` 遷移時決定，不在基線階段引入。
