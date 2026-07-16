## Why

目前專案同時保留 Compose Multiplatform 依賴與 Android 端 Compose BOM 導入方向，會讓 Compose 版本來源分裂，增加 catalog、plugin 與 UI layer 的管理成本。既然 Android UI 會以 Jetpack Compose 與 Compose BOM 為主，應移除 CMP 路線，讓 shared/core 維持純 KMP，iOS UI 由 SwiftUI 消費共享邏輯。

## What Changes

- **BREAKING**: 移除 Compose Multiplatform 作為 optional UI layer，不再保留 `org.jetbrains.compose` plugin、CMP runtime/foundation/ui/material/resources/tooling aliases 或 shared CMP UI entrypoint。
- Android UI 依賴改以 Jetpack Compose artifacts 搭配 `androidx.compose:compose-bom` 管理版本，避免同時由 CMP version 與 BOM 管理 Compose 版本。
- Android app 驗收只要求可成功編譯並在裝置或模擬器開啟 app，不要求新增 Compose UI 測試。
- `shared` 不再承擔共用 Compose UI；保留為純 KMP shared logic module，公開 API 不暴露 `@Composable`、`Modifier`、Compose state、Painter 或 Compose resources。
- iOS entrypoint 不再透過 `ComposeUIViewController` 啟動 shared UI；iOS 端以 SwiftUI 直接消費 KMP model、Flow/StateFlow、suspend API 或 facade。
- 更新既有 dependency catalog 規格，將「CMP 暫時保留」改為「CMP 必須移除且不得重新引入到 shared/core」。

## Capabilities

### New Capabilities

- 無。

### Modified Capabilities

- `kmp-dependency-catalog`: 將 CMP 保留策略改為移除策略，並要求 Android Compose 依賴由 Jetpack Compose BOM 對齊版本。

## Impact

- 受影響 module：`androidApp`、`shared`、root Gradle build configuration、`iosApp` entrypoint integration。
- 受影響檔案：`gradle/libs.versions.toml`、`build.gradle.kts`、`androidApp/build.gradle.kts`、`shared/build.gradle.kts`、`shared/src/commonMain`、`shared/src/iosMain/kotlin/com/shang/jetpackmoviekmp/MainViewController.kt`，以及可能引用 shared CMP UI 的 iOS app files。
- 受影響 dependency/plugin：移除 `compose-multiplatform` version/plugin alias 與 `org.jetbrains.compose.*` libraries；保留 Kotlin Compose Compiler plugin 給 Android Jetpack Compose 使用；新增或整理 AndroidX Compose BOM 與 AndroidX Compose aliases。
- 受影響驗證：`androidApp` 需可成功編譯，並能在裝置或模擬器啟動 app；本 change 不新增 UI 測試檔。
- 不涉及 `buildSrc`，目前專案使用 Version Catalog；若後續引入 convention plugin，需延續同一策略。
