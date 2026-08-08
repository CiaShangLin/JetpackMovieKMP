## Why

目前 `androidApp` 底部導覽是單一扁平 `NavBackStack`，用 `switchTab()`（MRU：移除目標 `NavKey` 後重新加到尾端）模擬 Tab 切換，所有 Tab 共用同一條 back stack。這與 Android 官方 Navigation 3 文件（[Common UI recipe](https://developer.android.com/guide/navigation/navigation-3/recipes/common-ui)）建議的 `TopLevelBackStack` 模式不同：官方模式讓每個頂層 Tab 各自維護獨立的 sub back stack，切換 Tab 不會影響其他 Tab 已累積的巡覽歷史，也不會把「Tab 本身」堆進返回鍵歷史。改採官方模式可讓底部導覽行為與官方建議一致，並為未來每個 Tab 需要保留較深 sub route 巡覽歷史的情境打好基礎。

## What Changes

- 新增 `TopLevelBackStack`（採官方 Common UI recipe 版本，泛型 `T` 對應專案既有 `NavKey`），取代 `MainActivity.kt` 目前的 `rememberNavBackStack(HomeKey)` 單一扁平 back stack。
- 移除 `MainNavBackStack.kt` 的 `switchTab()`（MRU reorder 邏輯），底部導覽 Tab 切換改呼叫 `TopLevelBackStack.addTopLevel()`。
- **BREAKING**：返回鍵行為改變——目前「依 Tab 造訪順序（MRU）逐一返回」的語意不再成立；改為「返回鍵只影響目前所在 Tab 的 sub route，Tab 切換本身不佔用返回鍵歷史」。
- `SuccessScreen()` 中「detail 顯示時隱藏 `JMNavigationSuiteScaffold`」的判斷邏輯，改用 `TopLevelBackStack.backStack.lastOrNull()` 是否為 `MovieDetailKey` 判斷，取代原本對扁平 `backStack.lastOrNull()` 的判斷（行為等價，但底層資料來源改變）。
- `mainEntry()` 的 `MovieDetailKey` push／pop，由 `backStack.add()` / `backStack.removeLastOrNull()` 改為 `TopLevelBackStack.add()` / `TopLevelBackStack.removeLast()`。
- 既有 `MainNavBackStackTest.kt` 針對 MRU 行為的測試案例移除，改為驗證 `TopLevelBackStack` 的對應行為。
- 不引入 process death 狀態恢復（官方 `NavigationState`／`rememberSerializable` 屬於更完整版本，不在本次範圍）。

## Capabilities

### New Capabilities

（無）

### Modified Capabilities

- `android-app-entry`：「`MainActivity` 主要導覽骨架 MUST 使用 Navigation3」這項 requirement 中，底部導覽 Tab 切換與返回鍵的 back stack 管理方式，由「MRU reordered 單一扁平 `backStack`」改為「`TopLevelBackStack`（每 Tab 各自獨立 sub back stack）」。「detail 顯示時 MUST 隱藏主 Navigation Suite」這項 requirement 的行為本身不變（僅底層資料來源從 `NavBackStack` 換成 `TopLevelBackStack.backStack`），不需要 spec 層級的 MODIFIED。

## Impact

- `androidApp`（Android-only，`main` 與 `test` source set）：
  - `androidApp/src/main/kotlin/com/shang/jetpackmoviekmp/ui/MainActivity.kt`：`rememberNavBackStack` 改用 `TopLevelBackStack`，`SuccessScreen()`／`mainEntry()` 的 back stack 操作方式調整。
  - `androidApp/src/main/kotlin/com/shang/jetpackmoviekmp/ui/MainNavBackStack.kt`：移除 `switchTab()`，改為存放（或改名為）`TopLevelBackStack` 類別定義。
  - `androidApp/src/main/kotlin/com/shang/jetpackmoviekmp/navigation/MainNavItem.kt`：角色不變（仍作為底部導覽 5 個 Tab 的定義來源），供 `TopLevelBackStack` 的 top-level keys 使用。
  - `androidApp/src/test/kotlin/com/shang/jetpackmoviekmp/ui/MainNavBackStackTest.kt`：既有 MRU 測試案例需改寫為 `TopLevelBackStack` 行為驗證。
- 不涉及 `shared/*` 任何模組（純 Android UI 層改動，不變更共用 business logic 或 public API）。
- 不涉及 iOS（`iosApp`、`ios-main-bottom-navigation` spec 不受影響）。
- 不新增或調整 `gradle/libs.versions.toml` 依賴，沿用既有 `androidx.navigation3:navigation3-runtime` 與 `androidx.navigation3:navigation3-ui` alias。
- 明確排除範圍：process death 狀態恢復（`NavigationState` + `rememberSerializable` 完整版）、Tab 間共享/傳遞參數、`reselectEvents`（重新點擊同一 Tab 重置捲動位置）等官方 recipe 的延伸功能，均不在本次變更範圍內。
