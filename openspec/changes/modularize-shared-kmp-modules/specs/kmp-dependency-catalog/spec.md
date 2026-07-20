## MODIFIED Requirements

### Requirement: 純 KMP 核心與 CMP UI 隔離
專案 MUST 移除 Compose Multiplatform 作為 shared UI layer，且純 KMP 的 Model、UseCase、Repository、Network、Database、DataStore 與 DI Core 不得依賴 Compose API、Compose resources 或 `org.jetbrains.compose` Gradle plugin。Android UI MUST 使用 Jetpack Compose 作為平台 UI；iOS UI MUST 使用 SwiftUI 或原生 iOS UI 消費 `shared:*` 子模組的 KMP 契約。

#### Scenario: 核心模組不得依賴 Compose
- **WHEN** 建立或調整 `shared:model`、`shared:domain`、`shared:data`、`shared:network`、`shared:database`、`shared:datastore`、`shared:common` 或 `shared:app` 的核心邏輯
- **THEN** 模組不套用 `org.jetbrains.compose`，且 dependencies 不包含 Compose UI、Runtime、Material 或 Resources

#### Scenario: 公開 API 支援 SwiftUI 消費
- **WHEN** iOS SwiftUI 消費 `shared:app` 匯出的 UseCase、Repository、ViewModel facade 或 state holder
- **THEN** 公開契約只暴露 Kotlin model、Flow/StateFlow、suspend API 或平台可橋接的 facade，且不得暴露 `@Composable`、Compose State、Modifier、Painter 或 Compose resources

#### Scenario: CMP 必須移除
- **WHEN** 檢查 Version Catalog、root plugins、`shared:*` 子模組、`androidApp` 與 iOS bridge
- **THEN** 不存在 `org.jetbrains.compose` plugin、Compose Multiplatform version alias、JetBrains Compose library aliases、`ComposeUIViewController` entrypoint 或 shared CMP UI resources

#### Scenario: 平台 UI 明確分流
- **WHEN** Android 或 iOS 建立 UI
- **THEN** Android 使用 Jetpack Compose 與 AndroidX Compose artifacts，iOS 使用 SwiftUI 或原生 iOS UI，兩者都只透過 `shared:app` 匯出的純 KMP API 取得資料與狀態

### Requirement: Ktor 網路 catalog 契約
Catalog MUST 提供 Ktor 3.5.1 的 Client Core、Content Negotiation、Kotlinx Serialization、Logging、MockEngine、CIO 與 Darwin aliases，且不得提供 Retrofit、OkHttp 或 Gson converter aliases。

#### Scenario: 排除舊網路堆疊
- **WHEN** 檢查 catalog 的網路與 Coil aliases
- **THEN** catalog 不包含 Retrofit、Retrofit converter、Retrofit coroutine adapter、OkHttp 或 `coil-network-okhttp`

#### Scenario: 網路測試 alias 可用
- **WHEN** `shared:network` 建立 commonTest
- **THEN** 可直接選用 Ktor MockEngine alias 而不新增硬編版本

### Requirement: Room KMP catalog 契約
Catalog MUST 提供目前官方可解析穩定版 Room 2.8.4 的 Gradle plugin、runtime、compiler、paging、KSP 與 SQLite 2.7.0 bundled driver aliases，並在設計中定義 Android/iOS target 的 KSP 配置規則；Room 3 穩定發布後 MUST 以獨立 change 切換。

#### Scenario: Room aliases 使用穩定座標
- **WHEN** 檢查 catalog 與 `shared:database` 模組
- **THEN** Room artifacts 使用 `androidx.room:room-*:2.8.4` 且 plugin 使用 `androidx.room`

#### Scenario: 所有目標有明確配置規則
- **WHEN** `shared:database` 導入 Room
- **THEN** 可依設計為 Android、iOS Arm64 與 iOS Simulator Arm64 配置 KSP compiler 與受版控 schema directory

#### Scenario: Room Paging integration 有對應 alias
- **WHEN** 某個子模組的 DAO 需要回傳 PagingSource
- **THEN** 可選用 `room-paging` alias

### Requirement: Catalog library 逐步引入與驗證
專案 MUST 依 `gradle/libs.versions.toml` 的 library alias 逐步引入依賴，且 MUST 優先處理支援 Kotlin Multiplatform 的依賴，再處理 Android-only 依賴；iOS-only 依賴除非為完成 KMP platform wiring 所需，否則 MUST 可略過。

#### Scenario: KMP library 優先引入
- **WHEN** 實作者開始導入尚未使用的 catalog library
- **THEN** 支援 common/KMP source set 的 library MUST 先於 Android-only library 被評估與引入

#### Scenario: Paging3 common 依賴納入對應子模組
- **WHEN** catalog 包含 Paging3 common 與 Android UI aliases
- **THEN** `androidx-paging-common` MUST 被加入需要分頁能力的 `shared:*` 子模組（例如 `shared:data`、`shared:database`）的 commonMain，且 Android UI paging aliases MUST 只加入 `androidApp` 或 Android UI source set

#### Scenario: Room KMP wiring 納入 shared:database
- **WHEN** catalog 包含 Room runtime、compiler、paging、SQLite bundled、Room plugin 與 KSP plugin aliases
- **THEN** `shared:database` MUST 套用 Room 與 KSP plugin，加入 Room KMP runtime/paging/SQLite dependencies，並為 Android 與 iOS KSP target 設定 Room compiler

#### Scenario: Room schema directory 受版控
- **WHEN** Room plugin 被套用到 `shared:database`
- **THEN** `shared:database` MUST 設定受版控 schema directory

#### Scenario: Android-only library 限制在 Android 範圍
- **WHEN** catalog library 只支援 Android 或 Android instrumentation
- **THEN** 該 library MUST 只被加入 `androidApp`、`androidMain`、`androidUnitTest` 或 Android instrumentation source set，不得加入任何 `shared:*` 子模組的 `commonMain`

#### Scenario: 每次引入後執行 Gradle 驗證
- **WHEN** 一個 library 或最小不可拆分 library group 已加入 build script
- **THEN** 實作者 MUST 立即執行對應 Gradle 編譯或測試任務，並確認沒有 dependency resolution、source set 或 plugin wiring 錯誤

#### Scenario: Gradle 成功後啟動 Android app
- **WHEN** 該輪 Gradle 驗證成功
- **THEN** 實作者 MUST 使用 `android-cli` 安裝並啟動 Android app，確認 app 可開啟到目前 entrypoint

#### Scenario: 單一 library 最多修復三次
- **WHEN** 某個 library 引入後的 Gradle 或 Android smoke test 失敗
- **THEN** 實作者 MUST 最多嘗試三次修復；若仍失敗，MUST 記錄失敗原因並跳過該 library，繼續評估下一個 library

#### Scenario: 跳過項目保留紀錄
- **WHEN** library 因不支援目前 KMP target、需要未規劃功能、環境阻塞或三次修復失敗而未引入
- **THEN** tasks 或實作紀錄 MUST 列出 library alias、跳過原因與後續建議

### Requirement: Ktlint Gradle 驗證任務
專案 MUST 提供可由 Gradle 執行的 `ktlintCheck` 任務，用來檢查 root、`androidApp` 與所有 `shared:*` 子模組的 Kotlin source 與 Kotlin Script 格式，且此任務 MUST 不修改 source。

#### Scenario: 執行 ktlintCheck
- **WHEN** 開發者從 repository root 執行 `gradlew.bat ktlintCheck`
- **THEN** Gradle MUST 使用專案宣告的 ktlint CLI dependency 檢查 Kotlin source 與 Kotlin Script
- **THEN** 任務 MUST 排除 build output

#### Scenario: ktlintCheck 不修改檔案
- **WHEN** `ktlintCheck` 發現格式不符合規範的 Kotlin 檔案
- **THEN** 任務 MUST 回報失敗
- **THEN** 任務 MUST NOT 自動改寫該檔案
