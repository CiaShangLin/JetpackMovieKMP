## Context

目前 Android 僅在頂層導覽提供少量 Compose `testTag`，iOS 僅有少數 accessibility label；電影、搜尋、收藏與設定控制項沒有跨平台穩定定位方式。兩端也都直接使用正式資料來源，因此測試旅程無法保證電影名稱、推薦內容與第二頁資料一致。

既有 `ui-test-discuss` 以 `docs/uitests/<journey>/journey.xml` 定義 Android 操作與驗收條件。此次將它提升為平台中立的旅程來源；Android 與 iOS 各自執行同一份操作意圖。

## Goals / Non-Goals

**Goals:**

- 以小而可獨立失敗的旅程覆蓋已確認的成功路徑。
- 對 Android 與 iOS 使用相同 selector 語意、固定資料與驗收結果。
- 讓首頁／搜尋的第二頁資料、詳情推薦、收藏與歷史資料可預測且可重置。
- 以 Android UI 自動化及 XCUITest 分別執行，保存各平台的截圖與結果。

**Non-Goals:**

- 不測試網路錯誤、重試、取消或正式 TMDB 回應。
- 不要求 iOS 在 `LanguageMode` 變更後動態切換 SwiftUI locale；語言旅程僅驗證設定值與復原。
- 不改變 production Repository、UseCase、資料庫 schema、TMDB API 或正式導覽行為。
- 不以同一套跨平台 UI driver 強行取代 Android 與 XCUITest 原生工具。

## Decisions

### 1. 以平台中立的 journey 作為測試合約

每個使用者行為維持一份小型 `journey.xml`，動作描述使用語意目標而非座標。Android 與 iOS 的執行器各自將 selector 解析成 Compose resource-id／語意節點或 `accessibilityIdentifier`／XCUITest query，並輸出平台分離的執行紀錄。

不直接共用 UI driver，因為 Android 既有流程使用 mobile-mcp / android-cli，而 iOS 需要 XCUITest 的生命週期、手勢與元素查詢能力。

### 2. selector 採用相同語意名稱

為共同控制項建立穩定名稱，例如 `nav_home`、`home_genre_<id>`、`movie_card_<id>`、`movie_collect_<id>`、`search_input`、`setting_theme`。Android 以 Compose semantics／`testTag` 暴露，iOS 以 `accessibilityIdentifier` 暴露；顯示文字、圖示與座標不得作為主要 selector。

### 3. 使用受控測試組裝與固定成功資料

測試啟動模式在組裝層選取固定 fixture，避免直接打 TMDB。fixture 必須提供固定首頁類型、`Spider-Man` 搜尋結果、電影詳情、推薦電影與至少兩頁首頁／搜尋清單；每次旅程前重置收藏、歷史及使用者設定。

fixture 只存在於測試組裝邊界，不改寫正式 Repository／UseCase 契約。若 Android 與 iOS 需要透過 `shared/app` 啟用，該 API 必須以明確測試用途命名並確認 Swift 匯出相容性。

### 4. 以獨立旅程保留必要的連續操作

啟動至首頁、詳情內滑至推薦區、收藏後切換收藏頁、以及主題／語言的復原皆屬不可拆的連續行為。首頁點擊與滑動類型切換、搜尋與搜尋結果開詳情、首頁與搜尋分頁則維持各自獨立旅程。

### 5. 語言驗證採設定值而非畫面翻譯

Android 選擇語言時會重建 Activity 並更新 locale；iOS 現況僅儲存 `LanguageMode`。共同旅程只驗證選擇英文後設定列顯示英文、再還原系統預設，不宣告兩端畫面文字必須即時翻譯。

## Risks / Trade-offs

- **[Risk] fixture 啟用方式意外進入正式環境** → Mitigation：僅由 test target／明確 launch argument 啟用，production 預設永遠使用正式組裝。
- **[Risk] iOS selector 與 Android selector 漂移** → Mitigation：將語意名稱列為 journey 前置契約，新增旅程時兩端同步檢查。
- **[Risk] 收藏與歷史資料造成案例互相污染** → Mitigation：每個旅程啟動前重置資料；跨頁驗證只在同一旅程內保留狀態。
- **[Trade-off] 固定資料不涵蓋正式 API 變動** → 這是 UI 互動回歸測試的刻意取捨；網路契約由既有 network/data 測試負責。

## Migration Plan

先加入 fixture 與 selector，再建立少量核心旅程並於兩端跑通；其後依序加入其餘旅程。若測試啟動模式有問題，可停止傳入測試 launch argument／移除測試組裝綁定，production 流程不受影響。

## Open Questions

- iOS 的 `journey.xml` 執行結果是否沿用既有 `summary.json` schema，或需要新增 XCUITest 特有的元素查詢欄位？
- 固定 fixture 應採 Android/iOS 各自 UI-test target 注入，還是由 `shared/app` 提供一個共用測試組裝入口？
