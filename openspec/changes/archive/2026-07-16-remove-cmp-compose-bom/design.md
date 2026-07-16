## Context

目前專案是 KMP 架構，`shared` 同時套用 Kotlin Multiplatform、Android KMP library、Compose Multiplatform 與 Compose Compiler plugin。`shared/commonMain` 依賴 `org.jetbrains.compose` runtime/foundation/ui/material/resources，iOS entrypoint 透過 `ComposeUIViewController { App() }` 啟動 shared Compose UI。`androidApp` 也套用 `org.jetbrains.compose`，並使用 CMP tooling aliases。

新的方向是 Android UI 採 Jetpack Compose，版本由 AndroidX Compose BOM 管理；iOS UI 採 SwiftUI，消費 shared/core 暴露的 KMP 邏輯。這代表 CMP 不再是 optional UI layer，而是需要從 build configuration 與 shared UI entrypoint 移除。

## Goals / Non-Goals

**Goals:**

- 移除 CMP plugin 與 `org.jetbrains.compose.*` dependencies，避免 CMP version 與 Android Compose BOM 同時管理 Compose 版本。
- 讓 `shared` 回到純 KMP logic module，不暴露 Compose UI API 或 Compose resources。
- Android 端保留 Jetpack Compose，但改由 AndroidX Compose BOM 統一 Compose artifact 版本。
- `androidApp` 驗收以成功編譯並可在裝置或模擬器開啟 app 為準，不新增 Compose UI 測試。
- iOS 端不再依賴 `ComposeUIViewController`，改由 SwiftUI 整合 KMP facade 或 model/state API。
- 維持既有 Repository / Use Case / MVVM 或 MVI 分層方向；UI framework 不進入 shared/core 契約。

**Non-Goals:**

- 不在本 change 搬移完整畫面或重建 Android UI 架構。
- 不在本 change 決定所有 iOS SwiftUI 畫面設計。
- 不改動資料庫 schema、network API key 策略或既有 domain behavior。
- 不引入 `buildSrc` 或 convention plugin；目前只整理 Version Catalog 與 Gradle scripts。

## Decisions

1. 移除 CMP，而不是繼續隔離保留。

   理由：目前需求已明確偏向 Android Jetpack Compose + Compose BOM。若繼續保留 CMP，Compose 版本會同時受 `compose-multiplatform` 與 BOM 影響，catalog 也需要維持 AndroidX 與 JetBrains Compose aliases 的雙軌命名，增加後續維護與升級成本。

   替代方案：保留 CMP 作為 optional shared UI。否決原因是這會延續版本來源分裂，也容易讓新 UI 或 resources 繼續進入 `shared/commonMain`。

2. Android Compose 依賴以 AndroidX Compose BOM 管理。

   理由：Android app 是目前主要 Compose UI 宿主，BOM 能讓 `ui`、`foundation`、`material3`、`ui-tooling-preview` 等 AndroidX Compose artifacts 對齊版本，不需要每個 alias 各自綁定版本。Kotlin Compose Compiler plugin 保留，因為 Jetpack Compose 仍需要 compiler plugin，但它不等同於 CMP plugin。

   替代方案：每個 AndroidX Compose artifact 各自指定版本。否決原因是版本升級時容易不一致，且不符合使用 Compose BOM 的目標。

3. `shared` 僅暴露跨平台邏輯與狀態契約。

   理由：保持 shared/core 純 KMP，Android 與 iOS 可以各自使用平台 UI。這遵循既有 Repository / Use Case / MVVM 或 MVI 分層方向，UI layer 只消費 immutable model、Flow/StateFlow、suspend API 或 facade，不把 Compose 型別放進 domain/data contract。

   替代方案：讓 shared 暴露 Compose runtime state 或 `@Composable` API。否決原因是 SwiftUI 消費困難，也會讓未來移除 UI framework 的成本變高。

4. iOS 改為 SwiftUI integration boundary。

   理由：移除 CMP 後，`ComposeUIViewController` 不再存在，iOS 必須由 SwiftUI 直接整合 shared framework。這會讓 iOS UI 決策回到 Apple 原生 UI stack，同時維持 KMP logic reuse。

## Risks / Trade-offs

- [移除 CMP 會讓現有 shared `App()` 無法直接被 Android/iOS 共用] -> Android 端若仍需要該畫面，需搬到 `androidApp` 或 Android UI module；iOS 端以 SwiftUI 重建畫面。
- [AndroidX Compose artifact 座標需從 JetBrains Compose 切換] -> 在 catalog 中明確分離 `androidx-compose-*` aliases，並以 BOM 驗證 Gradle resolution。
- [Compose Compiler plugin 仍保留可能被誤解為 CMP 未移除] -> 規格明確區分 Kotlin Compose Compiler plugin 與 `org.jetbrains.compose` CMP plugin；前者可用於 Android Jetpack Compose，後者必須移除。
- [iOS integration 可能暫時缺 UI entrypoint] -> 本 change 至少移除 CMP entrypoint 並保留 shared framework 可被 SwiftUI 消費；完整 iOS UI 可由後續 feature change 補齊。
- [不涉及 Room schema] -> 本 change 不改 database schema，因此不需要 Room migration。

## Migration Plan

1. 更新 Version Catalog：新增/整理 AndroidX Compose BOM 與 AndroidX Compose aliases，移除 CMP version、plugin alias 與 JetBrains Compose libraries。
2. 更新 root Gradle：移除 `libs.plugins.compose.multiplatform` apply false，保留 `libs.plugins.compose.compiler` 給 Android Compose。
3. 更新 `androidApp`：移除 CMP plugin，使用 AndroidX Compose BOM 與 AndroidX Compose dependencies。
4. 驗證 `androidApp` 可成功編譯，並可在裝置或模擬器啟動 app；不新增 Compose UI 測試檔。
5. 更新 `shared`：移除 CMP plugin、Compose dependencies、Android resources for CMP、`androidRuntimeClasspath(libs.compose.ui.tooling)` 與 shared Compose UI source。
6. 更新 iOS bridge：移除 `ComposeUIViewController` entrypoint，改提供 SwiftUI 可呼叫的 shared facade 或刪除不再使用的 CMP bridge。
7. 執行 Gradle compile/test/ktlint 驗證，確認沒有 `org.jetbrains.compose` dependency 或 plugin residue，且 `androidApp` 可啟動。

## Open Questions

- Android 端是否要在同一個 apply change 中把現有 `shared/App.kt` 搬到 `androidApp`，或只先移除 CMP 範本畫面並讓後續 UI change 重建？
- iOS 端是否已有 SwiftUI 畫面要立即串接 shared facade，或本 change 只保留 shared framework compile 通過？
