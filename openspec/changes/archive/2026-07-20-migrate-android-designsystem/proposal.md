## Why

舊 Android 專案的 `core/designsystem` 提供 Android Compose UI 基礎元件、主題與圖片資源。KMP 專案目前已將跨平台邏輯集中在 `shared:*` modules，`androidApp` 則作為 Android app 入口；若直接把 Android-only design system 放進 `shared`，會讓 iOS target 承擔不需要且可能無法編譯的 Android resource、`R.drawable`、`androidx.compose.ui.res.painterResource` 與 preview 依賴。

因此需要在 KMP 專案中建立 Android-only 的 core design system module，保留 UI 基礎層的可重用性，同時維持 `shared` 的跨平台邊界乾淨。

## What Changes

- 新增 Android library module `:core:designsystem`，實體路徑為 `core/designsystem`。
- module 與目錄命名一律使用全小寫 `designsystem`，避免 `designSystem` 造成 Gradle accessor 與檔案系統命名不一致。
- `:core:designsystem` 只提供 Android 使用，不加入 iOS framework export，也不放入 `shared` modules。
- `androidApp` 透過 `implementation(projects.core.designsystem)` 使用 design system。
- 將舊專案 `C:\Users\User\AndroidStudioProjects\JetpackMovieCompose\core\designsystem` 的 Android Compose 元件、theme 與 `res/drawable-*` 圖片資源遷移到新 module，並調整 package / namespace 為 KMP 專案命名空間。

## Capabilities

### Added Capabilities

- `android-designsystem-module`：定義 Android-only design system module 的位置、命名、依賴方向與平台邊界。

## Impact

- 受影響 module：`androidApp`、新增 `core:designsystem`。
- 受影響設定：`settings.gradle.kts`、`gradle/libs.versions.toml`（如缺少舊 design system 需要的 Compose / adaptive / Coil 依賴）。
- 不影響 iOS app 與 `shared:*` KMP modules。
- 不在本 change 內將 design system 改寫成 Compose Multiplatform common UI；若未來 iOS 也要共用 UI，需另開 change 評估 `shared:designsystem` 或 Compose Multiplatform resource 策略。
