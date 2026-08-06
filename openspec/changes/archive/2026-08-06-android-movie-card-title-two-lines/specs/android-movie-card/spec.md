## ADDED Requirements

### Requirement: 電影卡片標題 SHALL 固定佔用 2 行垂直空間

Android 電影卡片元件（`MovieCard` 的 `MovieTitle`）SHALL 不論標題實際文字行數為何，皆固定佔用 2 行文字的垂直空間，且標題超過 2 行時 SHALL 截斷不換行顯示第 3 行以後的內容。此行為 SHALL 與 iOS 端 `MovieCardView` 的 `titleSection`（`lineLimit(2, reservesSpace: true)`）保持一致，避免同一格線中卡片因標題行數不同而高度參差不齊。

#### Scenario: 標題文字僅 1 行時仍保留 2 行空間

- **WHEN** 電影標題文字寬度僅需 1 行即可完整顯示
- **THEN** 標題區塊 SHALL 仍佔用等同 2 行文字的垂直空間，第 2 行呈現為空白

#### Scenario: 標題文字剛好 2 行時完整顯示

- **WHEN** 電影標題文字寬度需要 2 行才能完整顯示
- **THEN** 標題區塊 SHALL 顯示完整 2 行文字，不留白也不截斷

#### Scenario: 標題文字超過 2 行時截斷

- **WHEN** 電影標題文字寬度需要 3 行以上才能完整顯示
- **THEN** 標題區塊 SHALL 只顯示前 2 行文字，第 2 行以省略號或裁切方式截斷，不得撐高卡片超過 2 行的高度

#### Scenario: 同一格線中不同標題行數的卡片高度一致

- **WHEN** 首頁 Grid 或清單同時顯示標題為 1 行與 2 行的電影卡片
- **THEN** 兩張卡片的標題區塊高度 SHALL 相同，卡片整體高度不因標題行數不同而參差不齊
