## ADDED Requirements

### Requirement: Android 搜尋 feature 模組
系統 SHALL 提供獨立的 Android `feature/search` 模組，並遵循既有 feature 模組的 Gradle、Koin、Compose 與 Navigation3 慣例；此模組 MUST 僅依賴既有 shared/core 模組與 version catalog 已管理的依賴。

#### Scenario: 建置搜尋功能模組
- **WHEN** Android App 編譯包含搜尋功能的版本
- **THEN** `feature/search` SHALL 以獨立 Android library 被納入 settings 與 androidApp 依賴，且不新增 iOS target 或 shared API 變更

### Requirement: 底部導覽搜尋入口
系統 SHALL 在 Android 底部導覽提供搜尋項目，並以 Navigation3 的 `SearchKey` 與 entry 顯示搜尋頁。

#### Scenario: 使用者切換至搜尋頁
- **WHEN** 使用者點擊底部導覽的搜尋項目
- **THEN** App SHALL 顯示搜尋頁，並將搜尋目的地作為目前 Navigation3 back stack 的目的地

### Requirement: 提交電影關鍵字搜尋
系統 SHALL 允許使用者在搜尋頁輸入電影名稱，並在使用者以 IME Search 提交非空關鍵字後，透過既有 `MovieRepository.getMovieSearchPager` 載入結果；空白 query MUST 不觸發網路搜尋。

#### Scenario: 提交有效搜尋字串
- **WHEN** 使用者輸入非空電影名稱並提交搜尋
- **THEN** 系統 SHALL 隱藏軟體鍵盤並顯示該關鍵字對應的可分頁電影結果

#### Scenario: 尚未提交搜尋
- **WHEN** 使用者首次進入搜尋頁且尚未提交非空關鍵字
- **THEN** 系統 SHALL 顯示引導使用者輸入電影名稱的初始提示，且不得建立搜尋請求

### Requirement: 搜尋結果與載入狀態
系統 SHALL 使用既有 MovieCard 與 grid 呈現搜尋結果，並為 Paging refresh 與 append 狀態提供可辨識的載入、錯誤與重試體驗。

#### Scenario: 初次搜尋載入中
- **WHEN** 已提交搜尋且 Paging refresh 處於載入狀態
- **THEN** 系統 SHALL 顯示載入畫面而非空白結果清單

#### Scenario: 初次搜尋失敗後重試
- **WHEN** 已提交搜尋的 Paging refresh 失敗且使用者點擊重試
- **THEN** 系統 SHALL 重新建立目前關鍵字的搜尋資料流並再次載入

#### Scenario: 載入更多結果失敗
- **WHEN** 使用者捲動結果清單且 Paging append 失敗
- **THEN** 系統 SHALL 在清單尾端顯示錯誤與重試操作，且重試 MUST 呼叫目前 Paging 資料的 retry

#### Scenario: 搜尋結果已到最後一頁
- **WHEN** Paging append 指示已到資料結尾
- **THEN** 系統 SHALL 在清單尾端顯示沒有更多資料的提示

### Requirement: 搜尋功能測試
系統 SHALL 為 SearchViewModel 的搜尋提交、空 query、重試與 query 切換行為提供 JVM 單元測試，並遵循 Arrange-Act-Assert 結構。

#### Scenario: 執行 feature/search 單元測試
- **WHEN** 開發者執行 `feature/search` 的 JVM 測試
- **THEN** 測試 SHALL 驗證 SearchViewModel 以 fake repository 正確建立或清空搜尋資料流，並驗證重試會重新取得目前 query 的 pager
