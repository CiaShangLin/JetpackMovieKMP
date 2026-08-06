## Why

`MainActivity.kt` 的底部導覽 Tab 切換邏輯（`SuccessScreen` 內 `onClick`）以 `backStack.removeLastOrNull()` + `backStack.add(item.key)` 取代目前項目，導致 `backStack` 永遠只維持 1 筆項目，無論切換過幾次 Tab。`androidx.navigation3.ui.NavDisplay` 內建的返回鍵攔截只在 `backStack.size > 1` 時啟用；backStack 恆為 1 筆的結果是攔截永遠停用，使用者切換底部 Tab 後按下返回鍵會直接觸發系統預設行為（結束 Activity），而非回到前一個瀏覽過的 Tab，體驗上等同「切一次 Tab 就少一次返回機會、隨時誤觸退出」。

## What Changes

- 修改底部導覽 Tab 切換邏輯：改為「Most-Recently-Used（MRU）reordered back stack」——切換到某個 Tab 時，若該 Tab 的 `NavKey` 已存在於 `backStack` 中，先移除舊有位置的項目，再將其加入 `backStack` 尾端（成為目前項目）；若不存在則直接加入。
- 效果：`backStack` 會依「使用者實際造訪過的 Tab 順序」保留最多 5 筆（對應 `MainNavItem` 的 5 個 Tab），返回鍵會依造訪順序（最近造訪的優先）逐一返回前一個 Tab，直到只剩 1 筆（初始／最先造訪的 Tab）才會交由系統預設行為結束 App。
- 不影響 `MovieDetailKey` 的 push／pop 邏輯（`backStack.add()` / `backStack.removeLastOrNull()`），詳情頁的返回行為維持現狀。
- 不引入自訂 `BackHandler` 或 `OnBackPressedDispatcher` 攔截，維持完全依賴 `NavDisplay` 內建的 predictive back 機制，僅調整 backStack 的內容管理方式。

## Capabilities

### New Capabilities

（無）

### Modified Capabilities

- `android-app-entry`：新增「底部導覽 Tab 切換 MUST 以 MRU reordered back stack 管理 backStack，返回鍵 MUST 依 Tab 造訪順序逐一返回」的需求，屬於既有「主要導覽骨架 MUST 使用 Navigation3」需求下的行為補強。

## Impact

- `androidApp`（Android-only，`src/main/kotlin`）
  - `com.shang.jetpackmoviekmp.ui.MainActivity.kt`：`SuccessScreen()` 內 `JMNavigationSuiteScaffold` 的 `onClick` 邏輯調整。
- 不涉及 `shared/*` 任何模組，不涉及 public API 變更，不影響 iOS 平台。
- 不新增或調整任何 `gradle/libs.versions.toml` 依賴。
- 範圍不包含：detail 頁（`MovieDetailKey`）的返回邏輯、「再按一次返回退出 App」提示、Tab 內部（非底部導覽切換）的巢狀導覽行為——這些維持現狀，不在本次變更範圍。
