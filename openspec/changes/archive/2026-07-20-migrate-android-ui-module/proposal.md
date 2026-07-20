## Why

舊 Android 專案的 `core/ui` 承載電影卡片、演員頭像、loading、error 與 paging 狀態等 Android Compose UI 元件。KMP 專案目前已有 Android-only `:core:designsystem` 討論產物，若 `core/ui` 沒有同步遷移，後續 Android 畫面只能直接複製 UI 程式碼到 `androidApp`，會讓 app entry 與可重用 UI 元件耦合。

因此需要在 KMP 專案建立 Android-only `:core:ui` module，延續舊專案的 Android UI module 邊界，同時維持 `shared:*` 只承載跨平台邏輯與模型。

## What Changes

- 新增 Android library module `:core:ui`，實體路徑為 `core/ui`。
- `:core:ui` 只提供 Android 使用，不加入 iOS framework export，也不由任何 `shared:*` module 依賴。
- `:core:ui` 依賴 Android-only `:core:designsystem`，並可依賴跨平台 `:shared:model` 與 `:shared:common`。
- 將舊專案 `C:\Users\User\AndroidStudioProjects\JetpackMovieCompose\core\ui` 的 Compose UI、Coil interceptor 與 Android resources 遷移到新 module。
- 將 package / namespace 從舊專案 `com.shang.ui` 調整為 `com.shang.jetpackmoviekmp.core.ui`。
- 將舊專案 Hilt-based `UiModule` 改為符合本 KMP 專案既有 DI 策略的 Koin module，或在首波遷移中明確排除 Hilt 綁定並以後續 app wiring 接上。
- 補齊 `gradle/libs.versions.toml` 中 `:core:ui` 所需且目前缺少的 Android UI 依賴，例如 Lottie Compose 與 Material Icons Extended。

## Capabilities

### New Capabilities

- `android-ui-module`：定義 Android-only `:core:ui` module 的位置、命名、平台邊界、依賴方向與可遷移 UI 元件範圍。

### Modified Capabilities

- 無。

## Impact

- 受影響 module：新增 `core:ui`，後續 Android app 或 Android feature modules 可依賴它；`androidApp` 可視整合需求加入 `implementation(projects.core.ui)`。
- 受影響設定：`settings.gradle.kts`、`gradle/libs.versions.toml`。
- 依賴方向：`:core:ui` 可依賴 `projects.core.designsystem`、`projects.shared.model`、`projects.shared.common`；`shared:*` modules 不可反向依賴 `projects.core.ui`。
- 不影響 iOS app 與 iOS framework export。
- 不在本 change 中把 `core/ui` 改寫成 Compose Multiplatform common UI。
