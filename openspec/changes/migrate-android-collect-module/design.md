## Context

`shared:data` 已透過 `MovieRepository` 提供收藏電影的即時 `Flow<List<MovieCardResult>>`，而 `core:ui` 已提供可顯示收藏狀態與回呼的 `MovieCard`。目前 `feature:home` 可切換收藏狀態，但 Android App 的 Navigation3 導覽僅註冊 `HomeKey`，`MainNavItem.COLLECT` 仍被註解，因此沒有可瀏覽收藏的 Android UI。

本次以來源專案 `JetpackMovieCompose/feature/collect` 的使用者行為為參考，但必須遵循本專案的 Android feature 分層：Koin 取代 Hilt、Navigation3 取代 classic Navigation Compose，並使用本專案的 package 與既有共用資料模型。iOS App 與 shared KMP API 均不在範圍內。

## Goals / Non-Goals

**Goals:**

- 建立可獨立編譯的 Android-only `feature:collect` 模組。
- 依既有 MVVM + Repository 模式，以 `CollectViewModel` 將收藏資料流轉成可渲染 UI state，並處理取消收藏。
- 以 Koin 注入 ViewModel、以 Navigation3 `CollectKey`／entry factory 整合至底部導覽。
- 沿用 `MovieCard`、`JMLazyVerticalGrid`、既有 theme 與 `MovieRepository`，使收藏變更即時反映畫面。
- 為 ViewModel 的收藏清單、空狀態與移除行為補 AAA 單元測試。

**Non-Goals:**

- 不新增或調整 `shared:data`、`shared:domain`、Room schema、資料庫 migration 或 iOS 收藏頁。
- 不遷移搜尋、歷史、設定等其餘尚未完成的底部導覽頁。
- 不改變收藏清單排序、加入多選／清空功能、電影詳情導覽或網路同步。

## Decisions

### 新增 Android-only feature module，沿用 `feature:home` 模組結構

新增 `feature/collect`，其 package 為 `com.shang.jetpackmoviekmp.feature.collect`，並包含 `ui`、`navigation`、`di`。Gradle 組態與必要依賴沿用 `feature:home` 的 Android library 與既有 version catalog alias，直接依賴 `core:designsystem`、`core:ui`、`shared:data`、`shared:model` 與 Koin。

選擇此作法是為了維持每個 Android 頁面可獨立遷移、編譯與接線的邊界。替代方案是把畫面直接放進 `androidApp`；這會讓 feature UI 與 app 入口耦合，且違反目前 `feature:home` 已建立的遷移模式，因此不採用。

### 以 MVVM 直接使用既有 `MovieRepository`

`CollectViewModel` 以 `getAllMovieCollect()` 建立 `StateFlow<CollectUiState>`；訂閱到空清單時由畫面顯示空狀態，非空時以 grid 顯示電影卡片。點擊已收藏卡片的收藏按鈕時，在 `viewModelScope` 內呼叫 `deleteMovieCollect()`；保留與 `HomeContentViewModel` 相同的 `MovieCardData` ↔ `MovieCardResult` 轉換與背景 dispatcher 行為。

這是遵循既有 MVVM + Repository 模式：目前 repository 已完整提供此畫面所需的讀寫 API，且 `shared:domain` 沒有收藏清單專用 UseCase。替代方案是為單一 Android UI 新增 UseCase；它會擴大 shared API 與測試範圍，沒有額外商業邏輯價值，因此不採用。

### Koin 與 Navigation3 依既有首頁模式接線

`collectModule()` 以 Koin `viewModel { }` 提供 `CollectViewModel`，由 `JetpackMovieApplication` 的 `loadKoinModules` 載入。`CollectNavigation.kt` 定義序列化的 `CollectKey : NavKey` 及 `collectEntry()`；`MainNavItem` 恢復 collect 項目，以 `CollectKey` 作為 key，並由 `MainActivity` 的 `NavDisplay` 對應到 collect entry。

選擇此作法可使 back stack 只保存 typed NavKey，與既有 `HomeKey` 一致。替代方案是採 classic Navigation Compose 的字串 route 或 Hilt ViewModel，皆與既有 Navigation3 / Koin 架構不相容，因此不採用。

### 空狀態資源置於 feature module

收藏頁的標題、空狀態文案與 `icon_empty.webp` 放在 `feature:collect`，使 feature 可自行封裝其 UI 資源；App 的底部導覽字串仍留在 `androidApp`。顯示層重用 `core:ui` 的 `MovieCard`，避免複製卡片元件。

## Risks / Trade-offs

- [收藏查詢未定義排序] → 本次保留既有 DAO 發出的順序，不新增隱含產品決策；未來需要排序時另立 change。
- [取消最後一筆收藏時畫面可能短暫轉換] → 以 Repository 的 reactive Flow 作為單一資料來源，下一次 emission 立即切換至空狀態，避免本地手動維護兩份清單。
- [新增 feature 接線遺漏會造成 Koin 或導覽執行期失敗] → 以 ViewModel unit test、`feature:collect` 模組編譯與 `:androidApp:assembleDebug` 驗證 settings、依賴、Koin module 與 Navigation3 接線。
- [來源專案實作與目前 API 不一致] → 僅遷移行為與視覺結構，所有 import、DI 與導覽 API 改用 KMP 專案既有實作。

## Migration Plan

1. 加入 `feature:collect` 至 Gradle settings，建立模組與 Koin / Navigation3 骨架。
2. 遷移 ViewModel、UI 與資源，改用現有 `MovieRepository`、`MovieCardData`、Koin API。
3. 在 `androidApp` 接上 Gradle 依賴、Koin module、`MainNavItem.COLLECT` 與 `NavDisplay` entry。
4. 執行 collect ViewModel 測試、`./gradlew :feature:collect:build`、`./gradlew :androidApp:assembleDebug` 與 `./gradlew ktlintCheck`。

本次無持久化資料或公開 API migration；若接線造成啟動問題，回退方式為移除 `feature:collect` 的 app 依賴、導航項與 Koin 載入，即可回復目前僅有首頁的行為。

## Open Questions

- 無阻塞問題；依來源專案行為，收藏頁僅支援取消收藏，不提供電影詳情導覽或排序控制。
