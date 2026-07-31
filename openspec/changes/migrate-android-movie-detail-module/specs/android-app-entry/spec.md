## ADDED Requirements

### Requirement: Android App MUST 支援電影詳情 Navigation3 目的地

`androidApp` MUST 以可序列化的 typed `MovieDetailKey(movieId)` 將電影詳情加入既有 Navigation3 `NavBackStack`，並由 `NavDisplay` entry provider 對應至 `feature:detail` 的 entry。首頁、搜尋、收藏、觀看紀錄與推薦電影的卡片點擊 MUST 導向此目的地。

#### Scenario: 從既有電影卡開啟 detail

- **WHEN** 使用者點擊首頁、搜尋、收藏或觀看紀錄中的電影卡
- **THEN** 系統 MUST 將包含該電影 id 的 `MovieDetailKey` 加入 back stack
- **AND** `NavDisplay` MUST 顯示對應的 detail entry，而非 `PlaceholderScreen`

#### Scenario: 從推薦電影開啟另一部 detail

- **WHEN** 使用者在 detail 頁點擊推薦電影卡
- **THEN** 系統 MUST 將推薦電影的 `MovieDetailKey` 加入目前 back stack 最後方

#### Scenario: 從巢狀 detail 返回

- **WHEN** 使用者在 detail 頁使用畫面返回按鈕或 Android 系統返回操作
- **THEN** 系統 MUST 只移除 back stack 最後一個 `MovieDetailKey`
- **AND** MUST 回到前一個 detail 或原本的入口頁

### Requirement: detail 顯示時 MUST 隱藏主 Navigation Suite

當 back stack 最後一個 destination 為 `MovieDetailKey` 時，`MainActivity` MUST 顯示 detail 內容而不包裝 `JMNavigationSuiteScaffold`；其他 root destination MUST 保持既有 Navigation Suite 行為。

#### Scenario: 開啟 detail 時不顯示主導航

- **WHEN** `MovieDetailKey` 為目前 back stack 最後一個 key
- **THEN** 畫面 MUST 不顯示 bottom navigation、navigation rail 或 navigation drawer

#### Scenario: 返回 root destination 後恢復主導航

- **WHEN** 使用者從 detail 返回且最後一個 key 為 Home、Search、Collect 或 History key
- **THEN** 系統 MUST 恢復既有 `JMNavigationSuiteScaffold` 與選取狀態
