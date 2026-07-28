## Context

PR #29 將 `MovieCardView` 的 `AsyncImage` 替換為 `RemoteAsyncImage`，後者以 `ZStack` 封裝 Kingfisher 圖片與 fallback 內容。海報的 3:4 比例仍套在遠端圖片元件上，但其 Loading／Error 內容沒有固定尺寸；在 `LazyVGrid` 進行欄寬量測時，卡片因此可能以不一致的固有尺寸參與排版。PR #30 僅將既有尺寸常數移至 `JMDesignTokens`，不是回歸根因。

目前工作目錄含先前嘗試的 `MovieCardView.swift` 未提交修改；實作階段必須以本設計與驗收情境重新檢視該修改，不得直接將其視為已完成修復。

## Goals / Non-Goals

**Goals:**

- 首頁雙欄 `LazyVGrid` 中的卡片在圖片載入前、成功後與失敗時維持相同欄寬與 3:4 海報高度。
- 相鄰卡片不得重疊、超出 ScrollView 水平內容範圍或因圖片狀態改變而跳動。
- 保留 `RemoteAsyncImage` 的 URL resolver、Kingfisher 快取與 Loading／Success／Error API。

**Non-Goals:**

- 不變更首頁的欄數、最小欄寬或 Design Token 值。
- 不更換 Kingfisher、TMDB URL 組合邏輯或 Shared/Kotlin API。
- 不處理海報下載失敗以外的網路重試策略。

## Decisions

### 由 `MovieCardView` 的海報容器擁有尺寸契約

海報區塊 SHALL 在外層 `ZStack` 建立固定 3:4 的可量測容器，再讓 `RemoteAsyncImage` 填滿該容器並裁切內容。評分與收藏按鈕維持 overlay，不得參與容器的寬高計算。

理由：卡片是唯一知道海報比例的呼叫端，將比例放在容器可讓 Loading、Success、Error 使用相同邊界。這符合目前 SwiftUI 共用元件只負責圖片載入、feature View 決定呈現尺寸的分工，沒有偏離既有 MVVM／共用 View 模式。

替代方案：在 `RemoteAsyncImage` 內硬編碼電影海報比例。此做法會使演員頭像或未來的橫幅圖片被限制為 3:4，因此不採用。

### `RemoteAsyncImage` 不得以 fallback 的固有尺寸決定父層大小

元件 SHALL 以可延展的容器呈現其 Loading、Success、Error 狀態；呼叫端提供有限寬高時，各狀態 MUST 填滿相同邊界。預設 fallback 僅繪製內容，不得要求額外最小寬高。

理由：共用圖片元件須可安全放入 `LazyVGrid`、`List` 與固定 frame，避免在非成功狀態改變父層量測結果。

替代方案：僅在首頁為 placeholder 加固定 `frame`。這會留下共用元件在其他呼叫端再次造成 layout 回歸的風險，因此不採用。

### 窄欄文字必須可在卡片邊界內排版

標題與上映日期區塊 SHALL 受卡片欄寬限制；日期可採單行縮放或截斷，但不得改變卡片最小寬度。卡片外層 SHALL 填滿格線提供的欄寬。

理由：長中文片名與日期加圖示的固有寬度可能超過小型 iPhone 的雙欄欄寬，即使海報尺寸已固定仍可能造成橫向溢出。

## Risks / Trade-offs

- [以 `minimumScaleFactor` 顯示日期可降低文字可讀性] → 僅用於日期，設定保守下限，並在最小寬度 iPhone 模擬器驗證。
- [SwiftUI 無法輕易對 View frame 撰寫純單元測試] → 將無 UI 相依的圖片狀態／尺寸策略抽成可測試值，並以 Xcode Preview 或模擬器截圖驗證實際雙欄版面。
- [未提交的先前修正可能與最終實作重複或衝突] → flow-apply 開始前先檢查 diff，依本設計保留、調整或移除。
