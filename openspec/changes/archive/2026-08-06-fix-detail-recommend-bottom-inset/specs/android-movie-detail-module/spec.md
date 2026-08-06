## ADDED Requirements

### Requirement: Detail 內容底部需消化系統導覽列 inset

detail 畫面內容 MUST 消化 `navigationBars` inset，確保 `LazyColumn` 內最後一個 item（包含推薦電影區塊的 `LazyRow`）不被系統導覽列遮擋。inset 處理 MUST 透過 scrollable 元件的 `contentPadding` 提供，MUST NOT 對 `LazyColumn` 的 parent container 使用 `Modifier.padding()`。

#### Scenario: 裝置啟用手勢導覽列或三鍵導覽列

- **WHEN** 使用者在 detail 畫面滑動到推薦電影區塊
- **THEN** 推薦電影卡片列表 MUST 完整顯示且不被系統導覽列遮擋
- **AND** 使用者 MUST 能完整點擊最後一排推薦電影卡片

#### Scenario: LazyColumn 仍可滑動延伸到系統導覽列後方

- **WHEN** 使用者持續向下滑動超過內容底部
- **THEN** `LazyColumn` MUST 仍可透過 `contentPadding` 滑入系統導覽列覆蓋區域（不因裁切內容而提前停止捲動）
