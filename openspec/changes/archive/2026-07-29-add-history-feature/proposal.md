## Why

舊專案 `JetpackMovieCompose` 的 `feature/history`（觀看歷史）尚未遷移到 `JetpackMovieKMP`。shared 層（`shared/database`、`shared/data`、`shared/domain`）已在先前遷移 `feature/collect` 時一併補齊了瀏覽紀錄所需的 entity、DAO、Repository 方法與 `GetHistoryMovieListUseCase`，且皆已有測試覆蓋；唯獨 Android UI 層的 `feature/history` 模組仍缺席，導致使用者無法在新專案上查看或清空觀看歷史。本次先完成 Android 部分，iOS 對應實作留待下一次任務處理。

## What Changes

- 新增 `feature:history` Android 模組，比照既有 `feature:collect` 的架構慣例（Koin 注入、Navigation3、Material3、`sealed class` UI state）重寫舊版 history 畫面
- `HistoryViewModel` 改用 `shared/domain` 既有的 `GetHistoryMovieListUseCase` 取得已合併收藏狀態的歷史清單，並沿用舊版「依目前收藏狀態切換收藏／取消收藏」與「清空全部歷史」邏輯，改呼叫既有 `MovieRepository` 方法
- 新增 `feature:history` 的 Koin module（`historyModule()`）與 Navigation3 進入點（`HistoryKey`／`historyEntry()`）
- 將 `androidApp` 的 `MainNavItem` 中既有、預留但被註解掉的 `HISTORY` 項目取消註解並接上新模組
- `MainActivity` 的 `NavDisplay` entryProvider 新增 `HistoryKey` 分支；`JetpackMovieApplication` 載入 `historyModule()`
- `androidApp/res/values/strings.xml` 補上 `nav_history` 字串
- **不變更** shared 層任何程式碼（database／data／domain 已具備完整 history 支援）
- **不包含** iOS 對應實作（留到下一次任務）

## Capabilities

### New Capabilities
- `android-history-module`：定義 `feature:history` 模組的建置、畫面、資料互動、Koin 注入與單元測試驗收標準

### Modified Capabilities
- `android-app-entry`：`MainNavItem`／`MainActivity` 的導覽骨架新增可進入觀看歷史頁的需求（比照既有收藏頁的導覽驗收標準）

## Impact

- 新增模組：`feature/history`
- 修改模組：
  - `androidApp`（`JetpackMovieApplication.kt`、`ui/MainActivity.kt`、`navigation/MainNavItem.kt`、`res/values/strings.xml`）
  - `settings.gradle.kts`（新增 `include(":feature:history")`）
- 依賴：`feature:history` 的 `build.gradle.kts` 比照 `feature:collect`，另外需引入 `implementation(projects.shared.domain)` 以使用 `GetHistoryMovieListUseCase`；所有依賴皆為既有 `gradle/libs.versions.toml` version catalog alias 與專案模組，不新增任何外部套件版本
- 不影響：`shared/database`、`shared/data`、`shared/domain`、`core/designsystem`、`core/ui`、iOS 端程式碼
