## ADDED Requirements

### Requirement: 首頁 SHALL 依載入狀態顯示對應畫面

`HomeView` SHALL 依 `HomeViewModel` 的 `uiState`（Loading／Success／Error）顯示對應畫面，比照 `SplashViewModel`／`SplashUiState` 既有的狀態機模式。

#### Scenario: 進入首頁時顯示 Loading 畫面
- **WHEN** 使用者切換到首頁 tab 且 `HomeViewModel` 尚未取得分類清單
- **THEN** `HomeView` SHALL 顯示 Loading 畫面

#### Scenario: 取得分類與電影清單成功後顯示內容
- **WHEN** `HomeViewModel` 成功取得分類清單與對應電影清單
- **THEN** `HomeView` SHALL 顯示 Tab + 分頁內容畫面，不再顯示 Loading 畫面

#### Scenario: 取得資料失敗時顯示失敗畫面
- **WHEN** `HomeViewModel` 取得分類清單或電影清單發生錯誤
- **THEN** `HomeView` SHALL 顯示含重試按鈕的失敗畫面

#### Scenario: 使用者點擊重試
- **WHEN** 使用者在失敗畫面點擊重試按鈕
- **THEN** `HomeViewModel` SHALL 重新觸發資料載入，並將 `uiState` 依新結果更新為 Loading／Success／Error 其中之一

### Requirement: 首頁 SHALL 以頂部 Tab 與橫向分頁呈現電影分類

首頁內容畫面 SHALL 在頂部顯示可切換的分類 Tab，並以橫向分頁（Pager）方式呈現各分類對應的電影清單，Tab 與分頁需保持選取狀態同步。

#### Scenario: 點擊 Tab 切換分頁
- **WHEN** 使用者點擊某個分類 Tab
- **THEN** 橫向分頁 SHALL 切換至該分類對應的頁面

#### Scenario: 手動滑動分頁同步 Tab
- **WHEN** 使用者手動左右滑動切換分頁
- **THEN** 頂部 Tab 的選取狀態 SHALL 同步更新為目前分頁對應的分類

### Requirement: 首頁分類分頁 SHALL 各自顯示對應分類的電影卡片清單

每個分類分頁 SHALL 使用 `ios-movie-card` 提供的電影卡片元件，以清單／格狀方式呈現該分類的電影，清單資料 SHALL 對應該分類的分類 id。

#### Scenario: 分頁顯示對應分類的電影
- **WHEN** 使用者切換到某個分類分頁
- **THEN** 該分頁 SHALL 顯示屬於該分類 id 的電影卡片清單

### Requirement: 首頁 SHALL 支援先以假資料開發、後串接真實 API 兩階段

`HomeViewModel` 的資料來源 SHALL 可在「假資料」與「串接 `shared` 端 UseCase 取得真實 API 資料」兩種模式間切換，且切換不應改變 `HomeUiState` 的狀態機或 `HomeView` 的畫面結構。

#### Scenario: 假資料模式下畫面可完整呈現
- **WHEN** `HomeViewModel` 使用內建假資料
- **THEN** `HomeView` SHALL 能完整顯示 Tab、分頁、電影卡片、Loading、Error 各種狀態，不需等待真實網路請求

#### Scenario: 串接真實 API 後行為一致
- **WHEN** `HomeViewModel` 改為呼叫 `shared` 端 UseCase 取得真實資料
- **THEN** `HomeView` 的 Tab／分頁／卡片／Loading／Error 呈現方式 SHALL 與假資料模式下的畫面結構一致，僅資料來源不同
