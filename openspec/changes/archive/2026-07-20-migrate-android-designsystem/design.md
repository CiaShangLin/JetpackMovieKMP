## Context

舊專案的 `core/designsystem` 是 Android library module，內容包含：

- Android `src/main/res/drawable-*` 圖片資源。
- `AndroidManifest.xml`。
- Compose UI 元件與 theme。
- `androidx.compose.ui.res.painterResource` 與 `R.drawable`。
- Android preview 與 Android instrumented test 目錄。

目前 KMP 專案的結構已將跨平台邏輯放在 `shared:*`，Android app 入口放在 `androidApp`。這次遷移的設計目標是讓 Android UI 基礎層可被 Android app 重用，同時不污染 `shared` 的 common / iOS 編譯邊界。

## Goals / Non-Goals

**Goals:**

- 建立 `:core:designsystem` 作為 Android-only design system module。
- 使用全小寫路徑 `core/designsystem` 與 Gradle path `:core:designsystem`。
- 讓 `androidApp` 只透過 `projects.core.designsystem` 依賴 design system。
- 保留 Android resource 支援，讓既有 loading / error drawable 可繼續由 Android resource 系統載入。
- 將 package / namespace 調整到 `com.shang.jetpackmoviekmp.core.designsystem`。

**Non-Goals:**

- 不把 design system 放入 `shared`。
- 不讓 iOS app 依賴或 export design system。
- 不在本 change 中把 Android resource 改成 Compose Multiplatform resource。
- 不重寫 UI 視覺設計或調整 feature screen 行為。
- 不遷移舊專案中與 design system 無關的 feature / data / domain 程式碼。

## Decisions

### 1. 使用 `:core:designsystem`，不使用 `:androidDesignSystem`

`core/designsystem` 延續舊專案 `core/designsystem` 的語意，也比單一 root module `androidDesignSystem` 更容易承接後續 Android core modules。此 module 仍明確是 Android-only，原因由 Gradle plugin 與 dependency direction 表達，而不是靠 module 名稱加上 `android` 前綴。

### 2. module 名稱使用全小寫 `designsystem`

Gradle module path 與實體目錄使用全小寫可降低命名不一致風險。定案命名：

```kotlin
include(":core:designsystem")
```

實體路徑：

```text
core/designsystem
```

type-safe accessor：

```kotlin
implementation(projects.core.designsystem)
```

### 3. Android-only module 不進 `shared`

舊 design system 使用 Android resource 與 Android Compose resource API。若放進 `shared/src/androidMain`，會讓 `shared` 同時承擔跨平台邏輯與 Android UI 基礎層，破壞目前 `shared:*` 的邊界；若做成 KMP module，也必須處理 iOS source set 與 resource 替代方案，超出本次遷移需求。

因此本次採用 Android library module：

```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.shang.jetpackmoviekmp.core.designsystem"
}
```

實際 plugin alias 以專案既有 `libs.versions.toml` 為準。

### 4. `androidApp` 保持 app composition root

`androidApp` 不直接承載 design system source code，只依賴 `:core:designsystem`。這讓 app entry、Application、Activity、navigation bootstrap 與 UI 基礎元件維持分離，後續新增 Android feature module 時也能共用同一套 design system。

## Risks / Trade-offs

- `core` 目前是新分層，會與既有 `shared` 分層並存；需要在 `settings.gradle.kts` 與文件中清楚表達 `core:*` 是 Android-only core modules。
- 舊 design system 若引用舊 package `com.shang.designsystem` 或舊專案專屬 dependency helper，需要在遷移時調整為本專案 package 與 version catalog。
- 若未來要支援 iOS 共用 UI，`core/designsystem` 可能需要拆分成 common token/theme 與 Android renderer；這應另開 change，不在本次 Android-only 遷移中預做抽象。
