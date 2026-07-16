## ADDED Requirements

### Requirement: Version Catalog 為依賴版本單一來源
專案 MUST 以 `gradle/libs.versions.toml` 管理所有外部 Gradle plugin 與 library 的明確版本，且 build scripts 不得重複宣告這些版本字串。

#### Scenario: Build script 使用 catalog alias
- **WHEN** 開發者檢查根目錄、shared 與 androidApp 的 build scripts
- **THEN** 所有受管理的外部 plugin 與 library 均透過 `libs.plugins.*` 或 `libs.*` alias 引用

#### Scenario: 禁止動態版本
- **WHEN** 開發者檢查 catalog 的版本值
- **THEN** catalog 不包含 `+`、動態範圍或 snapshot 版本

### Requirement: Catalog alias 反映依賴用途
專案 MUST 以一致的生態系前綴和用途命名 alias，並分開 runtime、compiler、test 與平台 engine，避免重現 `buildSrc` 大型依賴集合 helper。

#### Scenario: Ktor aliases 可獨立選用
- **WHEN** 模組只需要 Ktor core、JSON 或特定平台 engine
- **THEN** build script 可各自選用對應 alias，而不會被迫加入整組網路依賴

### Requirement: KMP source set 邊界
專案 MUST 將可共用依賴放入 `commonMain/commonTest`，並將 Android 或 iOS 專屬依賴限制在對應 platform source set 或 androidApp。

#### Scenario: Android-only library 不污染 commonMain
- **WHEN** 檢查 commonMain dependencies
- **THEN** Activity、AppCompat、SplashScreen、WorkManager、Chucker、Lottie、Android Material、Espresso 與 Android MockK 不存在於 commonMain

#### Scenario: 平台 engine 分流
- **WHEN** Ktor Client 同時支援 Android 與 iOS
- **THEN** Android source set 使用 CIO engine，iOS source set 使用 Darwin engine，commonMain 不依賴具體 engine

### Requirement: 純 KMP 核心與 CMP UI 隔離
專案 MUST 保留 CMP 作為選用 UI layer，且純 KMP 的 Model、UseCase、Repository、Network、Database、DataStore 與 DI Core 不得依賴 Compose API 或 Compose Gradle plugin。

#### Scenario: 核心模組不套用 Compose
- **WHEN** 後續建立或檢查 `core:model`、`core:domain`、`core:data`、`core:network`、`core:database`、`core:datastore` 或 `core:common`
- **THEN** 模組不套用 `org.jetbrains.compose`，且 dependencies 不包含 Compose UI、Runtime、Material 或 Resources

#### Scenario: 核心公開 API 可供 SwiftUI 消費
- **WHEN** iOS SwiftUI 呼叫共用 UseCase、Repository 或 ViewModel facade
- **THEN** 公開契約只使用純 Kotlin model、Flow/StateFlow 與 suspend API，不暴露 `@Composable`、Compose State、Modifier、Painter 或 Compose resources

#### Scenario: CMP 暫時保留
- **WHEN** 第一階段更新 Version Catalog 與 root plugins
- **THEN** Compose Multiplatform 與 Compose Compiler aliases 維持可解析，但不新增 CMP 畫面或讓核心模組引用它們

#### Scenario: 平台 UI 可獨立選擇
- **WHEN** Android 與 iOS 實作功能畫面
- **THEN** Android 可使用 Jetpack Compose、iOS 可使用 SwiftUI，兩者透過相同純 KMP 邏輯契約取得資料

### Requirement: Ktor 網路 catalog 契約
Catalog MUST 提供 Ktor 3.5.1 的 Client Core、Content Negotiation、Kotlinx Serialization、Logging、MockEngine、CIO 與 Darwin aliases，且不得提供 Retrofit、OkHttp 或 Gson converter aliases。

#### Scenario: 排除舊網路堆疊
- **WHEN** 檢查 catalog 的網路與 Coil aliases
- **THEN** catalog 不包含 Retrofit、Retrofit converter、Retrofit coroutine adapter、OkHttp 或 `coil-network-okhttp`

#### Scenario: 網路測試 alias 可用
- **WHEN** 後續 core/network change 建立 commonTest
- **THEN** 可直接選用 Ktor MockEngine alias 而不新增硬編版本

### Requirement: Koin 依賴注入 catalog 契約
Catalog MUST 提供 Koin 4.2.1 的 Core、Compose、Compose ViewModel 與 Test aliases，且不得提供 Hilt plugin、Hilt compiler 或 Hilt Android aliases。

#### Scenario: Common module 可選用 Koin
- **WHEN** 後續共用模組建立 repository、use case 或 ViewModel 的 Koin module
- **THEN** build script 可直接選用 KMP/Compose aliases 而不新增硬編版本

#### Scenario: 不導入預覽 compiler plugin
- **WHEN** 檢查第一階段 catalog plugins
- **THEN** 不包含仍為 RC 的 Koin compiler plugin

### Requirement: Room 3 KMP catalog 契約
Catalog MUST 提供穩定版 Room 3.0.0 的 Gradle plugin、runtime、compiler、paging、KSP 與 SQLite 2.7.0 bundled driver aliases，並在設計中定義 Android/iOS target 的 KSP 配置規則。

#### Scenario: Room 3 aliases 使用新座標
- **WHEN** 檢查 catalog 與 database module
- **THEN** Room artifacts 使用 `androidx.room3:room3-*` 且 plugin 使用 `androidx.room3`，不存在 `androidx.room:room-*` 2.x runtime/compiler

#### Scenario: 所有目標有明確配置規則
- **WHEN** 後續 core/database change 導入 Room
- **THEN** 可依設計為 Android、iOS Arm64 與 iOS Simulator Arm64 配置 KSP compiler 與受版控 schema directory

#### Scenario: Room Paging integration 有對應 alias
- **WHEN** 後續 DAO 需要回傳 PagingSource
- **THEN** 可選用 `room3-paging` alias 並依設計註冊 `PagingSourceDaoReturnTypeConverter`

### Requirement: KMP 基礎套件版本
專案 MUST 為 Coroutines 1.11.0、Kotlinx Serialization 1.11.0、Lifecycle 2.11.0、DataStore 1.2.1、Paging 3.5.0、Coil 3.5.0 與 Navigation 3 1.1.4 提供 catalog aliases，並只在有實際用途的 source set 引用。

#### Scenario: Coil 使用 Ktor 3
- **WHEN** 共用 UI 需要載入網路圖片
- **THEN** 使用 `coil-compose` 與 `coil-network-ktor3`，並重用已配置的平台 Ktor engine

#### Scenario: DataStore 不直接搬入 JVM Protobuf
- **WHEN** 第一階段建立 DataStore catalog 基線
- **THEN** commonMain 不加入 `protobuf-javalite`、`protobuf-kotlin-lite` 或 Protobuf Gradle plugin

### Requirement: 測試依賴分層
Catalog MUST 分開提供跨平台測試與 Android instrumentation aliases；後續核心遷移邏輯的單元測試 MUST 採 AAA 並達至少 80% 覆蓋率。

#### Scenario: Common test aliases 可跨平台使用
- **WHEN** 後續模組建立 commonTest 與 iOS tests
- **THEN** 可選用支援對應 KMP targets 的 Kotlin Test、Coroutines Test、Ktor Mock 與 Koin Test aliases

### Requirement: 基線建置驗證
專案 MUST 在完成 catalog 導入後驗證現有 Android 與 iOS 的主要編譯路徑，並在提交前通過 ktlintCheck。

#### Scenario: Android 與 iOS 編譯成功
- **WHEN** 執行專案定義的 Android compile、iOS metadata/framework compile 與 common tests
- **THEN** 所有現有 task 成功且沒有 catalog accessor 或 plugin resolution 錯誤

#### Scenario: 格式檢查成功
- **WHEN** 準備提交 catalog 與 build script 變更
- **THEN** ktlintCheck 成功
