## MODIFIED Requirements

### Requirement: 每個 tab 顯示對應內容，尚未實作的頁面顯示 placeholder text

首頁 tab SHALL 顯示 `ios-home-screen` 提供的實際首頁畫面（Tab + 分頁 + 電影卡片清單 + Loading／Error 狀態），不再顯示 placeholder text。收藏、搜尋、歷史、設定四個 tab 尚未實作對應資料流，SHALL 繼續顯示可辨識的 placeholder text。

#### Scenario: 切換到首頁 tab
- **WHEN** 使用者選取首頁 tab
- **THEN** 畫面內容 SHALL 顯示 `ios-home-screen` 提供的實際首頁畫面，不得顯示 `main_home_placeholder` 文字

#### Scenario: 切換到收藏 tab
- **WHEN** 使用者選取收藏 tab
- **THEN** 畫面內容 SHALL 顯示收藏 placeholder text

#### Scenario: 切換到搜尋 tab
- **WHEN** 使用者選取搜尋 tab
- **THEN** 畫面內容 SHALL 顯示搜尋 placeholder text

#### Scenario: 切換到歷史 tab
- **WHEN** 使用者選取歷史 tab
- **THEN** 畫面內容 SHALL 顯示歷史 placeholder text

#### Scenario: 切換到設定 tab
- **WHEN** 使用者選取設定 tab
- **THEN** 畫面內容 SHALL 顯示設定 placeholder text
