## MODIFIED Requirements

### Requirement: 純 KMP 核心與 CMP UI 隔離
專案 MUST 移除 Compose Multiplatform 作為 shared UI layer，且純 KMP 的 Model、UseCase、Repository、Network、Database、DataStore 與 DI Core 不得依賴 Compose API、Compose resources 或 `org.jetbrains.compose` Gradle plugin。Android UI MUST 使用 Jetpack Compose 作為平台 UI；iOS UI MUST 使用 SwiftUI 或原生 iOS UI 消費 shared/core 的 KMP 契約。

#### Scenario: 核心模組不得依賴 Compose
- **WHEN** 建立或調整 `core:model`、`core:domain`、`core:data`、`core:network`、`core:database`、`core:datastore`、`core:common` 或 `shared` 的核心邏輯
- **THEN** 模組不套用 `org.jetbrains.compose`，且 dependencies 不包含 Compose UI、Runtime、Material 或 Resources

#### Scenario: 公開 API 支援 SwiftUI 消費
- **WHEN** iOS SwiftUI 消費 shared/core 的 UseCase、Repository、ViewModel facade 或 state holder
- **THEN** 公開契約只暴露 Kotlin model、Flow/StateFlow、suspend API 或平台可橋接的 facade，且不得暴露 `@Composable`、Compose State、Modifier、Painter 或 Compose resources

#### Scenario: CMP 必須移除
- **WHEN** 檢查 Version Catalog、root plugins、`shared`、`androidApp` 與 iOS bridge
- **THEN** 不存在 `org.jetbrains.compose` plugin、Compose Multiplatform version alias、JetBrains Compose library aliases、`ComposeUIViewController` entrypoint 或 shared CMP UI resources

#### Scenario: 平台 UI 明確分流
- **WHEN** Android 或 iOS 建立 UI
- **THEN** Android 使用 Jetpack Compose 與 AndroidX Compose artifacts，iOS 使用 SwiftUI 或原生 iOS UI，兩者都只透過 shared/core 的純 KMP API 取得資料與狀態

## ADDED Requirements

### Requirement: Android Compose BOM 管理
Android UI module MUST 使用 AndroidX Compose BOM 管理 Jetpack Compose artifact 版本，避免同時由 Compose Multiplatform version 與 AndroidX Compose BOM 管理 Compose 相依性。

#### Scenario: Android Compose dependencies 使用 BOM
- **WHEN** `androidApp` 或 Android UI module 宣告 Compose UI、Foundation、Material3、Tooling Preview 或 Tooling dependencies
- **THEN** dependencies 透過 `platform(libs.androidx.compose.bom)` 對齊版本，且 individual Compose artifacts 不各自宣告版本

#### Scenario: CMP aliases 不得重新引入
- **WHEN** 更新 `gradle/libs.versions.toml` 或 Gradle build scripts
- **THEN** 不得新增 `compose-multiplatform` version、`org.jetbrains.compose` plugin alias 或 `org.jetbrains.compose.*` library alias

### Requirement: Android app 編譯與啟動驗證
`androidApp` MUST 在移除 CMP 並導入 AndroidX Compose BOM 後可成功編譯，且可在裝置或模擬器開啟 app；本 change 不要求新增 Compose UI 測試。

#### Scenario: Android app 可成功編譯
- **WHEN** 執行 `androidApp` 的 debug compile 或 assemble 任務
- **THEN** build 成功，且不依賴 Compose Multiplatform plugin 或 JetBrains Compose artifacts

#### Scenario: Android app 可開啟
- **WHEN** 將 `androidApp` 安裝到裝置或模擬器並啟動
- **THEN** app 可開啟到最小 Android UI entrypoint，且不需要新增 Compose UI 測試檔
