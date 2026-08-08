## MODIFIED Requirements

### Requirement: `MainActivity` 主要導覽骨架 MUST 使用 Navigation3

`MainActivity` 的主要導覽骨架 MUST 使用專案既定的 `androidx.navigation3:navigation3-runtime` 與 `androidx.navigation3:navigation3-ui`（`NavDisplay`／entry provider），MUST NOT 依賴 classic `androidx.navigation:navigation-compose`（`NavHostController`／`NavHost`／`rememberNavController`）。已遷移的首頁、收藏頁、歷史頁與設定頁 MUST 各自使用 typed `NavKey`，並由 `MainNavItem` 驅動底部導覽切換。底部導覽 Tab 切換 MUST 使用 `TopLevelBackStack`（每個 `MainNavItem` 對應的 `NavKey` 各自維護獨立 sub back stack，並將所有 Tab 的 sub back stack 攤平為單一 flat list 供 `NavDisplay` 消費）管理：切換到某個 Tab 時 MUST 呼叫 `TopLevelBackStack.addTopLevel()`，保留該 Tab 原本累積的 sub back stack 內容不被清除；不同 Tab 的 sub back stack 彼此互不影響、互不覆蓋。使用者按下返回鍵時 MUST 只影響目前所在 Tab 的 sub back stack（呼叫 `TopLevelBackStack.removeLast()`）：若目前 Tab 的 sub back stack 還有多筆項目，MUST 只移除最後一筆並停留在同一個 Tab；若目前 Tab 的 sub back stack 只剩該 Tab 本身這一筆（已在根畫面），MUST 將該 Tab 從 `TopLevelBackStack` 中移除，並切換到其餘仍存在的 Tab 中最後被加入的一個，直到只剩最後一個 Tab 時才交由系統預設行為結束 `MainActivity`。

#### Scenario: 不存在 classic Navigation Compose 依賴

- **WHEN** 檢查 `androidApp/build.gradle.kts` 與 `MainActivity.kt` 的 import
- **THEN** MUST NOT 出現 `androidx.navigation:navigation-compose` 依賴或 `androidx.navigation.NavHostController`／`androidx.navigation.compose.NavHost`／`androidx.navigation.compose.rememberNavController` 的 import

#### Scenario: 使用 Navigation3 API 建立導覽骨架

- **WHEN** 檢查 `MainActivity.kt` 的導覽相關實作
- **THEN** MUST 使用 `TopLevelBackStack` 建立每個 Tab 各自的 sub back stack，並將其攤平後的 `backStack` 交給 `NavDisplay` 渲染
- **AND** `MainNavItem` 驅動的導覽列點擊事件 MUST 呼叫 `TopLevelBackStack.addTopLevel()`（而非字串路由 `navigate()`）切換畫面

#### Scenario: 導覽列可進入收藏頁

- **WHEN** 使用者點擊底部導覽列的收藏項目
- **THEN** `MainNavItem.COLLECT` 對應的 `CollectKey` MUST 成為 `TopLevelBackStack` 目前的 `topLevelKey`
- **AND** `NavDisplay` 的 entryProvider MUST 回傳渲染 `CollectScreen` 的 collect `NavEntry`，不得回退為 `PlaceholderScreen`

#### Scenario: 導覽列可進入歷史頁

- **WHEN** 使用者點擊底部導覽列的歷史項目
- **THEN** `MainNavItem.HISTORY` 對應的 `HistoryKey` MUST 成為 `TopLevelBackStack` 目前的 `topLevelKey`
- **AND** `NavDisplay` 的 entryProvider MUST 回傳渲染 `HistoryScreen` 的 history `NavEntry`，不得回退為 `PlaceholderScreen`

#### Scenario: 導覽列可進入設定頁

- **WHEN** 使用者點擊底部導覽列的設定項目
- **THEN** `MainNavItem.SETTING` 對應的 `SettingKey` MUST 成為 `TopLevelBackStack` 目前的 `topLevelKey`
- **AND** `NavDisplay` 的 entryProvider MUST 回傳渲染 `SettingScreen` 的 setting `NavEntry`，不得回退為 `PlaceholderScreen`

#### Scenario: 切換 Tab 保留該 Tab 原有的 sub back stack

- **WHEN** 使用者在 History Tab 內瀏覽至某個子路由後，切換到 Home Tab，再切換回 History Tab
- **THEN** `TopLevelBackStack` MUST 仍顯示 History Tab 先前瀏覽到的子路由畫面，MUST NOT 重置回 History Tab 的根畫面

#### Scenario: 不同 Tab 的 back stack 互不影響

- **WHEN** 使用者依序點擊底部導覽列從 Home 切到 Collect、再從 Collect 切到 Search
- **THEN** Home、Collect、Search 三個 Tab 各自的 sub back stack MUST 保持獨立存在於 `TopLevelBackStack` 中，彼此不互相清除或覆蓋內容

#### Scenario: 返回鍵只影響目前 Tab 的 sub back stack

- **WHEN** 目前所在 Tab 的 sub back stack 有多於 1 筆項目，使用者按下返回鍵
- **THEN** `TopLevelBackStack.removeLast()` MUST 只移除該 Tab sub back stack 的最後一筆
- **AND** 畫面 MUST 停留在同一個 Tab

#### Scenario: 在 Tab 根畫面按返回鍵切換到其他仍存在的 Tab

- **WHEN** 目前所在 Tab 的 sub back stack 只剩下該 Tab 本身這一筆（已在根畫面），且 `TopLevelBackStack` 中仍有其他 Tab 存在，使用者按下返回鍵
- **THEN** 該 Tab MUST 從 `TopLevelBackStack` 中移除
- **AND** 畫面 MUST 切換到其餘 Tab 中最後被加入的一個

#### Scenario: 只剩最後一個 Tab 時返回鍵維持既有退出行為

- **WHEN** `TopLevelBackStack` 中只剩下 1 個 Tab，且其 sub back stack 只剩該 Tab 本身這一筆，使用者按下返回鍵
- **THEN** 系統 MUST 交由預設行為結束 `MainActivity`（行為與變更前一致，不受本次修改影響）

## REMOVED Requirements

### Requirement: 依序造訪多個 Tab 後 backStack 保留造訪順序

**Reason**：此為單一扁平 `backStack` 搭配 MRU（Most-Recently-Used）reorder 邏輯特有的行為描述。改採 `TopLevelBackStack` 後，每個 Tab 各自維護獨立 sub back stack，「單一扁平 backStack 依 Tab 造訪順序排列」這個概念不再適用。

**Migration**：對應行為由「不同 Tab 的 back stack 互不影響」與「切換 Tab 保留該 Tab 原有的 sub back stack」兩個新 Scenario 取代。

#### Scenario: 依序造訪多個 Tab 後 backStack 保留造訪順序

- **WHEN** 使用者依序點擊底部導覽列從 Home 切到 Collect、再從 Collect 切到 Search
- **THEN** `backStack` 內容 MUST 依序為 `[HomeKey, CollectKey, SearchKey]`

### Requirement: 返回鍵依 Tab 造訪順序逐一返回

**Reason**：此為 MRU reorder 邏輯特有的返回鍵行為（依「最近造訪者優先」逐一切換 Tab）。改採 `TopLevelBackStack` 後，返回鍵只影響目前 Tab 的 sub back stack，不再依「Tab 造訪順序」逐一切換其他 Tab。

**Migration**：對應行為由「返回鍵只影響目前 Tab 的 sub back stack」與「在 Tab 根畫面按返回鍵切換到其他仍存在的 Tab」兩個新 Scenario 取代。

#### Scenario: 返回鍵依 Tab 造訪順序逐一返回

- **WHEN** `backStack` 為 `[HomeKey, CollectKey, SearchKey]`（目前顯示 Search 頁）且使用者按下返回鍵
- **THEN** `backStack` MUST 變為 `[HomeKey, CollectKey]`，畫面 MUST 回到 Collect 頁
- **AND** 使用者再次按下返回鍵時，`backStack` MUST 變為 `[HomeKey]`，畫面 MUST 回到 Home 頁
- **AND** 使用者第三次按下返回鍵時（`backStack` 只剩 1 筆），系統 MUST 交由預設行為結束 `MainActivity`

### Requirement: 重複切換同一 Tab 不產生重複項目

**Reason**：此為 MRU reorder 邏輯（移除舊有位置後重新加入尾端）特有的行為描述。`TopLevelBackStack` 用 `LinkedHashMap` 管理每個 Tab 各自的 sub back stack，天生不會因重複切換同一 Tab 而產生扁平 `backStack` 中的重複項目，不需要獨立的 Scenario 描述此限制。

**Migration**：無需額外行為對應；`TopLevelBackStack.addTopLevel()` 對已存在的 Tab 只會切換 `topLevelKey`，不會重複建立該 Tab 的 sub back stack。

#### Scenario: 重複切換同一 Tab 不產生重複項目

- **WHEN** `backStack` 為 `[HomeKey, CollectKey]` 且使用者點擊底部導覽列的 Home 項目
- **THEN** `backStack` MUST 變為 `[CollectKey, HomeKey]`（移除舊有的 `HomeKey` 位置後重新加入尾端），MUST NOT 出現 `[HomeKey, CollectKey, HomeKey]` 這種重複項目的結果

### Requirement: 未曾切換 Tab 時返回鍵維持既有退出行為

**Reason**：此 Scenario 描述的邊界情境（`backStack` 僅有初始 1 筆）已被「只剩最後一個 Tab 時返回鍵維持既有退出行為」這個新 Scenario 以 `TopLevelBackStack` 的語意重新涵蓋，原本以「未曾切換 Tab」為前提的描述方式不再適用。

**Migration**：對應行為由「主要導覽骨架」requirement 下的「只剩最後一個 Tab 時返回鍵維持既有退出行為」Scenario 取代。

#### Scenario: 未曾切換 Tab 時返回鍵維持既有退出行為

- **WHEN** App 啟動後 `backStack` 僅有初始的 `[HomeKey]` 一筆，使用者從未點擊過底部導覽列即按下返回鍵
- **THEN** 系統 MUST 交由預設行為結束 `MainActivity`（行為與變更前一致，不受本次修改影響）
