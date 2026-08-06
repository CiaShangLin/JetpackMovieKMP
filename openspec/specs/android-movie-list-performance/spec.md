## Purpose

定義 Android 端電影清單（首頁分類分頁、搜尋分頁、收藏清單、觀看歷史清單）在項目 key 化、收藏狀態對應正確性，以及海報圖片依顯示情境請求對應尺寸這三方面的行為契約，確保清單捲動與收藏操作時項目身分穩定、圖片載入成本合理。

## Requirements

### Requirement: Paging 電影清單 SHALL 以電影 id 作為穩定 key

`feature/home` 首頁分類分頁清單與 `feature/search` 搜尋結果分頁清單，SHALL 使用 `LazyPagingItems` 對應的電影 id 作為清單項目的穩定 key（例如透過 `itemKey`），MUST NOT 只依賴清單項目目前所在的位置索引作為項目身分依據。

#### Scenario: 分頁清單插入新項目時既有項目身分不受影響

- **WHEN** 首頁分類分頁清單或搜尋結果分頁清單在使用者捲動觸發載入更多後，於既有項目前後插入新的電影項目
- **THEN** 既有已顯示的電影項目 SHALL 仍對應到同一部電影的 key，不因位置索引位移而被視為不同項目

#### Scenario: 分頁清單為空或載入失敗時不產生重複 key 例外

- **WHEN** 分頁清單目前沒有任何項目，或載入中的 placeholder 項目與已載入項目同時存在
- **THEN** 清單渲染 MUST NOT 因 key 重複或缺少 key 而擲出例外

### Requirement: 收藏清單與觀看歷史清單 SHALL 以電影 id 作為穩定 key

`feature/collect` 收藏清單與 `feature/history` 觀看歷史清單，SHALL 在其 `LazyVerticalGrid` 的 `items` 呼叫中，以清單項目的電影 id 作為穩定 key。

#### Scenario: 移除收藏電影後其餘收藏項目身分不受影響

- **WHEN** 使用者在收藏清單中取消收藏其中一部電影，該電影從收藏清單移除
- **THEN** 清單中其餘電影項目 SHALL 仍對應到各自原本的電影 id，不因清單項目數量減少而錄到錯誤的電影資料

#### Scenario: 清空觀看歷史後清單正確顯示空狀態

- **WHEN** 使用者清空觀看歷史，觀看歷史清單資料變為空清單
- **THEN** 畫面 SHALL 正確切換為空狀態顯示，MUST NOT 因先前項目的 key 殘留造成顯示異常

### Requirement: 電影卡片收藏狀態變更 SHALL 正確對應到使用者操作的電影項目

四個清單畫面（首頁、搜尋、收藏、觀看歷史）中，使用者對某一張電影卡片觸發收藏或取消收藏操作後，SHALL 只有該部電影對應的卡片收藏標記改變，清單中其他電影卡片的收藏標記與其他顯示內容 MUST NOT 受影響。

#### Scenario: 清單中觸發單一電影的收藏切換只影響該電影

- **WHEN** 清單同時顯示多部電影卡片，使用者點擊其中一部電影卡片的收藏按鈕
- **THEN** 該部電影的收藏圖示 SHALL 切換為對應的已收藏／未收藏狀態，其餘電影卡片的收藏圖示與內容 MUST NOT 改變

#### Scenario: 清單重新整理或分頁資料更新後收藏標記仍對應正確電影

- **WHEN** 分頁清單因收藏資料變化而重新標記各電影的收藏狀態
- **THEN** 每張電影卡片顯示的收藏標記 SHALL 對應該卡片實際顯示的電影，MUST NOT 對應到清單中其他電影

### Requirement: 電影清單縮圖 SHALL 依清單顯示情境請求對應尺寸的 TMDB 圖片

`MovieCard` 在清單情境（首頁、搜尋、收藏、觀看歷史、電影詳情頁推薦清單）顯示的海報縮圖，SHALL 向 TMDB 圖片服務請求適合清單縮圖顯示的圖片尺寸，MUST NOT 一律請求 TMDB 最大原始（`original`）解析度圖片。未明確指定清單縮圖尺寸提示的既有圖片顯示情境（例如電影詳情頁的 backdrop 大圖），SHALL 維持請求原始尺寸圖片的既有行為。

#### Scenario: 清單卡片縮圖請求較小尺寸圖片

- **WHEN** `MovieCard` 在任一清單畫面顯示電影海報縮圖
- **THEN** 實際發出的圖片請求 SHALL 使用清單縮圖對應的 TMDB 圖片尺寸，而非 `original` 尺寸

#### Scenario: 詳情頁大圖維持原始尺寸

- **WHEN** 電影詳情頁顯示 backdrop 大圖
- **THEN** 實際發出的圖片請求 SHALL 維持請求 TMDB `original` 尺寸，行為與改動前一致
