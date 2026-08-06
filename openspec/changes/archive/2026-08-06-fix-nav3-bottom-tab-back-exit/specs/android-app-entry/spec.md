## MODIFIED Requirements

### Requirement: `MainActivity` 主要導覽骨架 MUST 使用 Navigation3

`MainActivity` 的主要導覽骨架 MUST 使用專案既定的 `androidx.navigation3:navigation3-runtime` 與 `androidx.navigation3:navigation3-ui`（`NavBackStack`／`NavDisplay`／entry provider），MUST NOT 依賴 classic `androidx.navigation:navigation-compose`（`NavHostController`／`NavHost`／`rememberNavController`）。已遷移的首頁、收藏頁、歷史頁與設定頁 MUST 各自使用 typed `NavKey`，並由 `MainNavItem` 驅動底部導覽切換。底部導覽 Tab 切換 MUST 以「Most-Recently-Used（MRU）reordered back stack」管理共用的 `backStack`：切換到某個 Tab 時，若該 Tab 對應的 `NavKey` 已存在於 `backStack` 中，MUST 先移除該筆舊項目再將其加入 `backStack` 尾端；若不存在則直接加入尾端。使用者按下返回鍵時 MUST 依 Tab 造訪順序（最近造訪者優先）逐一回到前一個造訪過的 Tab，直到 `backStack` 只剩 1 筆才交由系統預設行為結束 `MainActivity`。

#### Scenario: 不存在 classic Navigation Compose 依賴

- **WHEN** 檢查 `androidApp/build.gradle.kts` 與 `MainActivity.kt` 的 import
- **THEN** MUST NOT 出現 `androidx.navigation:navigation-compose` 依賴或 `androidx.navigation.NavHostController`／`androidx.navigation.compose.NavHost`／`androidx.navigation.compose.rememberNavController` 的 import

#### Scenario: 使用 Navigation3 API 建立導覽骨架

- **WHEN** 檢查 `MainActivity.kt` 的導覽相關實作
- **THEN** MUST 使用 `rememberNavBackStack()` 建立 backstack 並交給 `NavDisplay` 渲染
- **AND** `MainNavItem` 驅動的導覽列點擊事件 MUST 透過操作該 backstack（而非字串路由 `navigate()`）切換畫面

#### Scenario: 導覽列可進入收藏頁

- **WHEN** 使用者點擊底部導覽列的收藏項目
- **THEN** `MainNavItem.COLLECT` 對應的 `CollectKey` MUST 成為目前 backstack 項目
- **AND** `NavDisplay` 的 entryProvider MUST 回傳渲染 `CollectScreen` 的 collect `NavEntry`，不得回退為 `PlaceholderScreen`

#### Scenario: 導覽列可進入歷史頁

- **WHEN** 使用者點擊底部導覽列的歷史項目
- **THEN** `MainNavItem.HISTORY` 對應的 `HistoryKey` MUST 成為目前 backstack 項目
- **AND** `NavDisplay` 的 entryProvider MUST 回傳渲染 `HistoryScreen` 的 history `NavEntry`，不得回退為 `PlaceholderScreen`

#### Scenario: 導覽列可進入設定頁

- **WHEN** 使用者點擊底部導覽列的設定項目
- **THEN** `MainNavItem.SETTING` 對應的 `SettingKey` MUST 成為目前 backstack 項目
- **AND** `NavDisplay` 的 entryProvider MUST 回傳渲染 `SettingScreen` 的 setting `NavEntry`，不得回退為 `PlaceholderScreen`

#### Scenario: 依序造訪多個 Tab 後 backStack 保留造訪順序

- **WHEN** 使用者依序點擊底部導覽列從 Home 切到 Collect、再從 Collect 切到 Search
- **THEN** `backStack` 內容 MUST 依序為 `[HomeKey, CollectKey, SearchKey]`

#### Scenario: 返回鍵依 Tab 造訪順序逐一返回

- **WHEN** `backStack` 為 `[HomeKey, CollectKey, SearchKey]`（目前顯示 Search 頁）且使用者按下返回鍵
- **THEN** `backStack` MUST 變為 `[HomeKey, CollectKey]`，畫面 MUST 回到 Collect 頁
- **AND** 使用者再次按下返回鍵時，`backStack` MUST 變為 `[HomeKey]`，畫面 MUST 回到 Home 頁
- **AND** 使用者第三次按下返回鍵時（`backStack` 只剩 1 筆），系統 MUST 交由預設行為結束 `MainActivity`

#### Scenario: 重複切換同一 Tab 不產生重複項目

- **WHEN** `backStack` 為 `[HomeKey, CollectKey]` 且使用者點擊底部導覽列的 Home 項目
- **THEN** `backStack` MUST 變為 `[CollectKey, HomeKey]`（移除舊有的 `HomeKey` 位置後重新加入尾端），MUST NOT 出現 `[HomeKey, CollectKey, HomeKey]` 這種重複項目的結果

#### Scenario: 未曾切換 Tab 時返回鍵維持既有退出行為

- **WHEN** App 啟動後 `backStack` 僅有初始的 `[HomeKey]` 一筆，使用者從未點擊過底部導覽列即按下返回鍵
- **THEN** 系統 MUST 交由預設行為結束 `MainActivity`（行為與變更前一致，不受本次修改影響）
