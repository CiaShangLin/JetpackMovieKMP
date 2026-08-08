## Context

目前 `MainActivity.kt` 用 `rememberNavBackStack(HomeKey)` 建立單一扁平 `NavBackStack<NavKey>`，底部導覽 Tab 切換由 `MainNavBackStack.kt` 的 `switchTab()`（MRU：移除目標 key 後重新加到尾端）驅動，所有 Tab 共用同一條 back stack。這個做法與 Android 官方 Navigation 3 [Common UI recipe](https://developer.android.com/guide/navigation/navigation-3/recipes/common-ui) 建議的 `TopLevelBackStack` 模式不同：官方模式讓每個頂層 Tab（`MainNavItem` 對應的 5 個 `NavKey`）各自維護獨立的 sub back stack，並攤平成一份 flat list 供 `NavDisplay` 消費。

使用者已在需求討論階段確認採用官方精簡版 `TopLevelBackStack`（自成一個 class，非 `NavigationState` + `Navigator` 完整版），本設計聚焦於如何把這個 class 整合進現有 `MainActivity.kt` 的三個既有耦合點：Tab 點擊事件、`SuccessScreen()` 隱藏主導航的判斷、`mainEntry()` 的 `MovieDetailKey` push/pop。

## Goals / Non-Goals

**Goals:**
- 每個底部導覽 Tab（Home／Collect／Search／History／Setting）各自維護獨立 sub back stack，切換 Tab 不影響其他 Tab 已累積的巡覽歷史。
- 完整沿用官方 Common UI recipe 的 `TopLevelBackStack` 實作，不客製化其內部行為。
- 維持「detail（`MovieDetailKey`）顯示時隱藏 `JMNavigationSuiteScaffold`」既有使用者體感行為。
- 維持 `mainEntry()` 的 entry 分派邏輯與各 feature 的 `NavEntry` 建構方式不變，只調整其呼叫的 back stack API。

**Non-Goals:**
- Process death 狀態恢復（官方 `NavigationState` + `rememberSerializable` 完整版才有此能力）。
- Tab 之間共享或傳遞導覽參數。
- `reselectEvents`（重新點擊同一 Tab 時重置捲動位置／重置子畫面狀態）。
- 調整 `MainNavItem` 定義的 Tab 清單、圖示或文案。

## Decisions

**1. 採用官方 `TopLevelBackStack` 精簡版，不採 `NavigationState` + `Navigator` 完整版**
精簡版用一個 class 自行管理 `LinkedHashMap<T, SnapshotStateList<T>>`，能以最小改動達成「每 Tab 獨立 sub back stack」的核心目標，且專案目前沒有 process death 恢復或 reselect 需求。符合「修改範圍聚焦在使用者要求的行為，避免無關重構」的專案規範。

**2. `TopLevelBackStack<T: Any>` 的泛型 `T` 直接綁定為既有 `androidx.navigation3.runtime.NavKey`**
不另外包裝型別，維持與現有 `mainEntry(navKey: NavKey, ...): NavEntry<NavKey>`、`NavDisplay` 的介面相容，`TopLevelBackStack.backStack`（`SnapshotStateList<NavKey>`）可直接傳給 `NavDisplay(backStack = ...)`。

**3. `TopLevelBackStack` 定義位置沿用 `MainNavBackStack.kt`（實作後因 ktlint 規則改名為 `TopLevelBackStack.kt`）**
改寫該檔案內容，移除 `switchTab()`，改為存放 `TopLevelBackStack` class。實作時發現 ktlint 的 `standard:filename` 規則要求檔名與其內含的單一 class 一致，因此將檔案由 `MainNavBackStack.kt` 改名為 `TopLevelBackStack.kt`（對應測試檔 `MainNavBackStackTest.kt` 同步改名為 `TopLevelBackStackTest.kt`）。維持既有作法：由 `MainActivity` 內 `remember { TopLevelBackStack(HomeKey) }` 建立實例（純 UI 層物件，不透過 Koin 注入，與現行 `rememberNavBackStack()` 的建立方式一致）。

**4. 「detail 顯示時隱藏主 Navigation Suite」改用 `topLevelBackStack.backStack.lastOrNull() is MovieDetailKey`**
`TopLevelBackStack.backStack` 本身就是攤平後的 flat list（給 `NavDisplay` 消費），語意與現行 `backStack.lastOrNull()` 判斷等價，只是資料來源從 `NavBackStack` 換成 `TopLevelBackStack.backStack`。

**5. Tab 切換與 `MovieDetailKey` push/pop 改呼叫 `TopLevelBackStack` 對應 API**
- 底部導覽 `onClick`：`switchTab(backStack, item.key)` → `topLevelBackStack.addTopLevel(item.key)`。
- `mainEntry()` 內 `backStack.add(MovieDetailKey(...))` → `topLevelBackStack.add(MovieDetailKey(...))`（加入目前 `topLevelKey` 對應的 sub stack）。
- `onBack` / detail 返回：`backStack.removeLastOrNull()` → `topLevelBackStack.removeLast()`。
- `JMNavigationSuiteScaffold` 的 `selected` 判斷：`currentKey == item.key` 改用 `topLevelBackStack.topLevelKey == item.key`（僅在 Navigation Suite 顯示時才會用到，與「是否隱藏」的判斷分屬不同資料來源，見 Risks）。

## Risks / Trade-offs

- **[Risk]** 返回鍵語意改變：目前 spec 定義的「依 Tab 造訪順序（MRU）逐一返回其他 Tab」行為不再成立，改為「返回鍵只影響目前 Tab 的 sub back stack」。→ **Mitigation**：已於 proposal 標記 **BREAKING**；spec delta 需明確 MODIFIED 對應 requirement 與 scenario，避免驗收時誤判為未達成原需求。

- **[Risk]** 官方 `TopLevelBackStack.removeLast()` 在「目前 Tab 的 sub stack 只剩 Tab root 這 1 筆時再次呼叫」會把該 Tab 整個從 `topLevelStacks` 移除，並將 `topLevelKey` 切到 `topLevelStacks.keys.last()`（即 map 中最後一個仍存在的 Tab，不是「上一個造訪的 Tab」）。→ **Mitigation**：這是官方 recipe 的原生行為，本次不客製化調整；design 與 spec scenario 需明確描述此行為，讓「使用者在某 Tab 根畫面按返回鍵」的實際結果可被驗證，避免與舊有 MRU 行為的預期混淆。

- **[Risk]** 既有 `MainNavBackStackTest.kt` 針對 MRU 行為（重複造訪不產生重複項目、依序返回等）的測試案例會全部失效。→ **Mitigation**：tasks 中列出需改寫的測試案例，改為驗證 `TopLevelBackStack` 的 per-tab 獨立 sub stack 行為。

- **[Risk]** `JMNavigationSuiteScaffold` 的 `selected` 判斷資料來源（`topLevelKey`）與「是否隱藏」判斷資料來源（`backStack.lastOrNull()`）不同，未來若有人只改其中一處會造成不一致。→ **Mitigation**：design 中明確定義兩者用途分工（`backStack.lastOrNull()` 判斷是否顯示 Navigation Suite；`topLevelKey` 只在 Navigation Suite 顯示時用於判斷目前選取的 Tab），並於程式碼註解註明兩者不可互相替代的原因。

## Migration Plan

純 Android UI 層改動，不涉及使用者資料或資料庫 schema，隨下一次一般 App release 生效，不需要 feature flag 或漸進式 rollout。若上線後發現導覽行為迴歸，透過 revert commit／PR 即可回滾，無需額外資料遷移或相容處理。

## Open Questions

無——使用者已於需求討論階段確認採用精簡版 `TopLevelBackStack`，且 proposal 已明確排除 process death 恢復、Tab 間參數共享與 `reselectEvents`。
