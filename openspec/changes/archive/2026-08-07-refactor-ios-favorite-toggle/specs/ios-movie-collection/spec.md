## MODIFIED Requirements

### Requirement: iOS 收藏操作 SHALL 由共用的收藏切換元件執行

iOS 收藏相關 ViewModel 不得各自直接持有 `MovieRepository` 並自行判斷該呼叫 `insertMovieCollect()` 還是 `deleteMovieCollect()`；SHALL 改為委派給共用的 `MovieCollectToggler`，`MovieCollectToggler` SHALL 以建構子接收 `MovieCollectionToggling`（僅涵蓋 insert/delete 兩支方法的窄介面，實作以 `MovieRepositoryCollectionAdapter` 轉接具體 `MovieRepository`）；View 與 `MovieCardView` 不得直接取得 Koin 依賴。每個 ViewModel SHALL 持有各自獨立的 `MovieCollectToggler` 實例，不得跨 ViewModel 共用同一個實例，以維持「同一時間同一畫面只允許一次收藏切換」的防連點邊界互不干擾。

#### Scenario: 將未收藏電影加入收藏

- **WHEN** 收藏操作收到目前收藏狀態為 `false` 的電影
- **THEN** `MovieCollectToggler` SHALL 呼叫 `MovieCollectionToggling.insertMovieCollect()`，後續 shared Flow 發出包含該電影的收藏清單

#### Scenario: 將已收藏電影移除收藏

- **WHEN** 收藏操作收到目前收藏狀態為 `true` 的電影
- **THEN** `MovieCollectToggler` SHALL 呼叫 `MovieCollectionToggling.deleteMovieCollect()`，後續 shared Flow 不再包含該電影

#### Scenario: 連續快速觸發同一個收藏切換

- **WHEN** 同一個 `MovieCollectToggler` 實例的 `toggle()` 尚在執行中，使用者再次觸發同一張卡片的收藏切換
- **THEN** 第二次呼叫 SHALL 被防連點機制擋下、不得同時對同一筆資料重複發出 insert/delete 呼叫

#### Scenario: 收藏寫入失敗

- **WHEN** `MovieCollectionToggling.insertMovieCollect()` 或 `deleteMovieCollect()` 拋出錯誤
- **THEN** `MovieCollectToggler` SHALL 攔截該錯誤、不得讓例外向上傳播造成 App crash
