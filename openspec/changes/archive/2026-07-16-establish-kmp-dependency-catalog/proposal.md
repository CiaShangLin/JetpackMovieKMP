## Why

JetpackMovieKMP 需要在保留原 JetpackMovieCompose 專案的前提下，先建立一套可供 Android 與 iOS 共用、可追蹤且可逐步遷移的 Gradle 依賴基線。舊專案以 `buildSrc` 混合管理版本、依賴 helper 與 Android convention，若直接複製會把 Android-only 套件與過時設定帶進 KMP，因此第一階段應先完成依賴盤點、分類及 Version Catalog 契約。

## What Changes

- 以 `gradle/libs.versions.toml` 作為外部 plugin 與 library 版本的單一來源，補齊明確、可搜尋且按用途分組的 aliases。
- 建立舊依賴的處置清單，區分 `commonMain`、平台 source set、Android app、測試與暫不遷移項目。
- 導入 KMP 網路基線：以 Ktor Client、Content Negotiation、Kotlinx Serialization 與平台 engine 取代 Retrofit/OkHttp API client。
- 導入 Koin KMP/Compose 依賴，以 DSL module 取代 Hilt plugin、compiler 與 Android annotation；本階段不採用預覽中的 Koin compiler plugin。
- **BREAKING**：不導入 Retrofit、OkHttp、Hilt、Gson converter 與 Retrofit coroutine adapter aliases；後續搬移的程式碼必須改用 Ktor/Koin API。
- **BREAKING**：Room 由 `androidx.room` 2.6.1 升級至目前官方可解析的穩定版 `androidx.room` 2.8.4，並搭配 Room plugin、KSP 與 bundled SQLite；後續若 Room 3 穩定發布，再另立 change 切換座標與 package。
- 保留與升級可在 KMP 使用的 Kotlin Coroutines、Kotlinx Serialization、Lifecycle、DataStore、Paging、Coil 及 Compose Multiplatform 依賴；Android-only 的 WorkManager、Chucker、Lottie、SplashScreen 與 instrumentation test 依賴只允許放在 Android source set 或 `androidApp`。
- 保留目前 CMP plugin 與 aliases，但將 CMP 定位為選用 UI layer；本階段不新增共用 UI，也不強迫 iOS 採用 CMP。
- 建立架構邊界：UseCase、Repository、Model、Ktor、Room、DataStore 與 Koin Core 必須維持純 KMP，不得依賴 Compose；Android UI 預設使用 Jetpack Compose，iOS UI 可使用 SwiftUI。
- 不直接搬移 `buildSrc` 的 `DependenciesVersions`、`Dependencies`、`DependenciesProvider`；產品 flavor、簽章、build type 與共用 Gradle convention 另案處理。

## Capabilities

### New Capabilities

- `kmp-dependency-catalog`: 定義 KMP 依賴盤點、版本選擇、Version Catalog aliases、source set 邊界與替代套件的驗收要求。

### Modified Capabilities

無。

## Impact

- 新專案建置檔：`gradle/libs.versions.toml`、根目錄 `build.gradle.kts`、`shared/build.gradle.kts`、`androidApp/build.gradle.kts`，以及必要時的 `gradle.properties`。
- 後續預計建立的純 KMP 模組：`core/network`、`core/database`、`core/datastore`、`core/data`、`core/domain`、`core/model`、`core/common`；平台 UI 與選用的 CMP UI 必須位於這些核心模組之外。本 change 僅建立它們可依循的 catalog 與 source set 基線。
- 舊專案僅作唯讀盤點；`JetpackMovieCompose/buildSrc` 的 `DependenciesVersions.kt`、`Dependencies.kt`、`DependenciesProvider.kt` 不會被修改或複製。
- 依賴/API 影響：Retrofit/OkHttp 改為 Ktor，Hilt 改為 Koin，Room 先升級到 2.8.4 穩定版；既有攔截器、DI modules、DAO/database 與測試日後需逐模組改寫。
- 平台範圍：目前 Android、iOS Arm64 與 iOS Simulator Arm64；catalog alias 必須標示共用或平台限定用途。
