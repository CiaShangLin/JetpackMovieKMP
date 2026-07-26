## ADDED Requirements

### Requirement: 電影卡片 UI 資料模型 SHALL 定義於 shared/model，供雙平台共用

電影卡片顯示所需的 UI 資料模型（原 `core/ui.MovieCardData`，含 `movieCardId`／`movieCardTitle`／`movieCardPosterPath`／`movieCardReleaseDate`／`movieCardVoteAverage`／`movieCardIsCollect`／`movieCardTimestamp` 欄位）SHALL 定義於 `shared/model`，Android `core/ui` 與 iOS SHALL 共用同一份型別定義，MUST NOT 各自維護欄位相同的重複型別。

#### Scenario: Android core/ui 使用共用型別
- **WHEN** 檢查 `core/ui` 內電影卡片相關程式碼（`MovieCard.kt`、`feature/home` 對應呼叫端）的 import
- **THEN** SHALL 使用 `shared/model` 提供的電影卡片 UI 資料模型，MUST NOT 於 `core/ui` 內重新定義同欄位的本地型別

#### Scenario: iOS 電影卡片元件使用共用型別
- **WHEN** 檢查 iOS `MovieCardView` 的輸入參數型別
- **THEN** SHALL 為 `shared/model` 提供的電影卡片 UI 資料模型（經 SKIE 匯出），MUST NOT 是另外定義、欄位重複的 iOS-only struct

### Requirement: 電影卡片 UI 資料模型與 MovieCardResult 的轉換 SHALL 隨型別搬移一併保留

原有的 `MovieCardResult` ↔ 電影卡片 UI 資料模型雙向轉換函式（原 `asMovieCardResult()`／`asMovieCardData()`）SHALL 隨型別搬移到 `shared/model`，轉換邏輯與欄位對應 MUST 與搬移前一致，不得在搬移過程中變更既有欄位對應關係。

#### Scenario: 搬移後轉換行為不變
- **WHEN** 對同一筆 `MovieCardResult` 資料分別呼叫搬移前後的轉換函式
- **THEN** 產生的電影卡片 UI 資料模型欄位值 SHALL 完全一致
