## ADDED Requirements

### Requirement: iOS 圖片元件 SHALL 透過 BaseHostUrlProvider 補齊 TMDB 相對圖片路徑

iOS 專用圖片元件 SHALL 接受圖片路徑或完整 URL，並透過 shared 匯出的 `BaseHostUrlProvider` 取得目前 TMDB 圖片 base host。當輸入為相對路徑且 base host 不為空字串時，元件 SHALL 產生可供 Kingfisher `KFImage` 載入的完整 URL；當輸入已是 `http://` 或 `https://` 完整 URL 時，元件 MUST 保留原始 URL。

#### Scenario: 相對路徑會補上 base host 與 original size
- **WHEN** base host 為 `https://image.tmdb.org/t/p/` 且圖片路徑為 `/poster.jpg`
- **THEN** iOS 圖片元件 SHALL 使用 `https://image.tmdb.org/t/p/original/poster.jpg` 載入圖片

#### Scenario: 完整 URL 不會被改寫
- **WHEN** 圖片輸入為 `https://example.com/poster.jpg`
- **THEN** iOS 圖片元件 SHALL 使用 `https://example.com/poster.jpg` 載入圖片

#### Scenario: base host 尚未存在時不產生錯誤的拼接 URL
- **WHEN** base host 為空字串且圖片路徑為 `/poster.jpg`
- **THEN** iOS 圖片元件 MUST NOT 產生 `original/poster.jpg` 或其他錯誤拼接 URL，並 SHALL 進入 Error 狀態或顯示錯誤 fallback

### Requirement: iOS 圖片元件 SHALL 使用 Kingfisher 提供圖片快取

iOS 專用圖片元件 SHALL 以 Kingfisher 作為底層圖片載入器，並使用 Kingfisher 的記憶體與磁碟快取能力載入相同 URL 的圖片。

#### Scenario: 相同 URL 可重用快取
- **WHEN** 同一個完整圖片 URL 在列表重繪或重新進入頁面時再次被載入
- **THEN** iOS 圖片元件 SHALL 交由 Kingfisher 從既有快取或下載流程取得圖片，不得自行以 SwiftUI `AsyncImage` 重新下載

#### Scenario: Feature View 不直接依賴 Kingfisher API
- **WHEN** `MovieCardView` 或其他 feature View 顯示遠端圖片
- **THEN** SHALL 使用 iOS 共用圖片元件，不得直接散落使用 `KFImage`

### Requirement: iOS 圖片元件 SHALL 提供 Loading、Success、Error 三種狀態呈現

iOS 專用圖片元件 SHALL 將 Kingfisher 圖片載入結果收斂為 Loading、Success、Error 三種狀態，並提供預設 UI 與呼叫端可覆寫的內容區塊。

#### Scenario: 圖片載入中顯示 Loading 狀態
- **WHEN** Kingfisher 尚未取得成功或失敗結果
- **THEN** iOS 圖片元件 SHALL 顯示 Loading 內容

#### Scenario: 圖片載入成功顯示 Success 狀態
- **WHEN** Kingfisher 成功取得圖片
- **THEN** iOS 圖片元件 SHALL 顯示 Success 圖片內容，並允許呼叫端套用顯示樣式

#### Scenario: 圖片載入失敗顯示 Error 狀態
- **WHEN** URL 無效、base host 不可用或 Kingfisher 載入失敗
- **THEN** iOS 圖片元件 SHALL 顯示 Error 內容

### Requirement: iOS 圖片元件 MUST 透過建構子注入依賴

iOS 專用圖片元件 MUST 透過建構子參數或父層注入取得 URL resolver／`BaseHostUrlProvider`，不得在 View 內部直接呼叫 `KoinHelper` 或自行啟動 Koin。

#### Scenario: Preview 可使用假的 URL resolver
- **WHEN** 建立 SwiftUI Preview 或單元測試
- **THEN** 呼叫端 SHALL 能傳入假的 URL resolver 或固定 base host，而不需要啟動 shared Koin

#### Scenario: 正式 App 從組裝根注入 provider
- **WHEN** iOS App 已透過 `doInitKoinIos` 啟動 shared Koin
- **THEN** 組裝根或父層 SHALL 能透過 shared accessor 取得 `BaseHostUrlProvider` 並傳入圖片元件消費路徑
