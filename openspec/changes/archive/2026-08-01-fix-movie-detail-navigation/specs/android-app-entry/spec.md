## MODIFIED Requirements

### Requirement: Android App MUST 支援電影詳情 Navigation3 目的地

`androidApp` MUST 以可序列化的 typed `MovieDetailKey(movieId)` 將電影詳情加入既有 Navigation3 `NavBackStack`，並由 `NavDisplay` entry provider 對應至 `feature:detail` 的 entry。首頁、搜尋、收藏、觀看紀錄與推薦電影的卡片點擊 MUST 導向此目的地。實際掛載在 `NavDisplay` 的 entry provider（`mainEntry()`）MUST 直接處理 `MovieDetailKey`，MUST NOT 只存在於未被任何呼叫端使用的替代 entry provider 中。

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

#### Scenario: 實際生效的 entry provider 直接處理 MovieDetailKey

- **WHEN** 檢查 `MainActivity.kt` 中實際傳給 `SuccessScreen` 內 `NavDisplay` 的 `entryProvider`
- **THEN** 該 entry provider 的實作（`mainEntry()`）MUST 包含 `MovieDetailKey` 分支並回傳 `feature:detail` 的 entry
- **AND** repo 內 MUST NOT 存在其他處理 `MovieDetailKey` 但未被任何 `NavDisplay` 呼叫的孤立 entry provider（例如未被使用的重複 Composable）
