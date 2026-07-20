## Context

舊專案的 `core/ui` 是 Android library module，主要內容包含：

- Compose UI 元件：`MovieCard`、`MovieActor`、`LoadingScreen`、`ErrorScreen`、`MovieListPagerScreen`。
- UI 資料轉換：`MovieCardData` 與 `MovieCardResult` 互轉。
- Coil request interceptor：`HostInterceptor` 透過 `BaseHostUrlProvider` 補齊 TMDB 圖片 host。
- Android resources：`raw/loading.json`、`drawable-*/icon_actor_placeholder.webp`、多語系 `strings.xml`。
- 舊專案 Hilt module：`UiModule` 提供 `ImageLoader`。

目前 KMP 專案已有 `shared:common` 的 `BaseHostUrlProvider`、`shared:model` 的 `MovieCardResult`，以及 Android-only `:core:designsystem` 的 openspec 設計。`core/ui` 應建立在這些邊界之上，而不是放進 `shared` 或直接塞進 `androidApp`。

## Goals / Non-Goals

**Goals:**

- 建立 `:core:ui` 作為 Android-only UI component module。
- 使用全小寫路徑 `core/ui` 與 Gradle path `:core:ui`。
- 將 package / namespace 調整為 `com.shang.jetpackmoviekmp.core.ui`。
- 遷移舊 `core/ui/src/main` 的可重用 UI 元件與 Android resources。
- 讓 `:core:ui` 依賴 `:core:designsystem`、`:shared:model`、`:shared:common`，避免複製 model 或 common provider。
- 移除舊專案 Hilt 耦合，改用本專案既有 Koin 方向處理 `ImageLoader` wiring。

**Non-Goals:**

- 不把 `core/ui` 放入 `shared`。
- 不讓 iOS app 依賴或 export `core/ui`。
- 不在本 change 中把 Android resources 改為 Compose Multiplatform resources。
- 不遷移舊專案 feature screen、navigation、repository、use case 或 domain/data 程式碼。
- 不重新設計 UI 視覺，不調整電影卡片、錯誤畫面或 loading 畫面的產品行為。

## Decisions

### 1. 使用 Android-only `:core:ui`

`core/ui` 會使用 Android library 與 Kotlin Android plugin，因為舊程式碼使用 Android resources、`androidx.compose.ui.res.stringResource`、`painterResource`、Compose preview、Coil Android `ImageLoader.Builder(context)` 與 Lottie raw resource。這些都不是目前 `shared:*` common source set 的自然職責。

定案命名：

```kotlin
include(":core:ui")
```

實體路徑：

```text
core/ui
```

type-safe accessor：

```kotlin
implementation(projects.core.ui)
```

### 2. `:core:ui` 依賴 `:core:designsystem`

舊 `MovieCard` 與 `MovieActor` 使用 `JMAsyncImage` 與 `StarRatingColor`，這些應由 `:core:designsystem` 提供。`core:ui` 不應複製 design system 元件，也不應反向讓 design system 依賴 UI module。

依賴方向：

```text
androidApp / Android feature modules
  -> core:ui
  -> core:designsystem
  -> Android Compose / resources

core:ui
  -> shared:model
  -> shared:common
```

### 3. Package 與 imports 需要調整為 KMP 專案命名空間

舊 package `com.shang.ui` 應改為：

```kotlin
package com.shang.jetpackmoviekmp.core.ui
```

舊 imports 需要對應調整：

- `com.shang.model.MovieCardResult` -> `com.shang.jetpackmoviekmp.model.MovieCardResult`
- `com.shang.common.BaseHostUrlProvider` -> `com.shang.jetpackmoviekmp.common.BaseHostUrlProvider`
- `com.shang.designsystem.*` -> `com.shang.jetpackmoviekmp.core.designsystem.*`
- `com.shang.ui.R` -> `com.shang.jetpackmoviekmp.core.ui.R`

### 4. Hilt binding 不帶入本專案

舊 `UiModule` 使用 Hilt annotation 與 `@ApplicationContext`。本 KMP 專案目前使用 Koin 啟動與 module 組裝，因此新 `:core:ui` 不應新增 Hilt 依賴。首波遷移採以下其中一種實作策略：

- 建立 `core.ui.di.UiModule` 的 Koin module，提供 `HostInterceptor` 與 `ImageLoader`。
- 若 `ImageLoader` 尚未被 app wiring 使用，先遷移 `HostInterceptor` 與 UI 元件，將 Koin wiring 作為同 change 的後續任務完成。

不接受在本 change 中引入 Hilt 作為新 DI 框架。

### 5. 依賴補齊以版本 catalog 為準

舊 `core/ui` 透過自訂 `deps.*` helper 引入 AndroidX、Lottie、Hilt、designsystem/model/common module。KMP 專案使用 `gradle/libs.versions.toml`，因此遷移時要將缺少的依賴加到 version catalog，再在 `core/ui/build.gradle.kts` 使用既有 alias。

預期需要檢查：

- Compose BOM、runtime、foundation、material3、ui、ui-tooling-preview。
- `androidx.paging:paging-compose`。
- Coil Compose / Coil network Ktor3。
- Lottie Compose。
- Material Icons Extended。
- Koin Android / Compose，如 Koin module 需要 Android context wiring。

## Risks / Trade-offs

- `:core:ui` 依賴 `:core:designsystem`，所以實作順序必須在 design system module 可編譯之後；若 design system change 尚未 apply，`core:ui` 需等待或同批處理。
- 舊 `MovieListPagerScreen` 的 refresh state 判斷可能存在既有行為問題，但本 change 目標是遷移 module 邊界，不主動改動產品行為；若要修正 paging UX 應另開 bug-fix change。
- Lottie 與 Material Icons Extended 會新增 Android app 體積與依賴面，需只加入 `core:ui` 實際使用的依賴。
- `HostInterceptor` 依賴同步 `BaseHostUrlProvider.getBaseHostUrl()`；若未來 provider 改成 suspend/Flow，需要另行調整 Coil interceptor 設計。
