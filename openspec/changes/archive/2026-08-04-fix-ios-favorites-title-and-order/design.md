## Context

`MovieCollectEntity` 已持有 `timestamp`，但 `MovieCollectDao.getAllMovies()` 未指定排序。Android 收藏頁直接顯示 repository emission，並未額外排序；iOS 同樣直接顯示該 emission，且缺少標題。Android 的 `MovieCard` 在點擊收藏時才建立時間戳，iOS 則把既有卡片的預設值傳入，因此目前資料來源不是跨平台一致的收藏時間。

此變更橫跨 shared 資料層、Android 收藏頁與 iOS 收藏頁，維持既有 Repository 與 MVVM 架構，不新增 UI 端排序邏輯。

## Goals / Non-Goals

**Goals:**

- 讓任一平台新增收藏後都持久化同一語意的「收藏當下時間」。
- 讓所有 `getAllMovieCollect()` 消費者取得最新收藏在前的穩定契約。
- 讓 iOS 收藏頁補齊與歷史頁一致的在地化標頭。
- 以 DAO、repository 與 iOS UI 測試鎖住此行為。

**Non-Goals:**

- 不新增使用者可選擇的排序或篩選器。
- 不變更瀏覽紀錄的排序語意。
- 不變更資料表 schema、資料庫版本或既有收藏的欄位值。
- 不在 Android 或 iOS ViewModel／View 再做重複排序。

## Decisions

### 由 shared Repository 建立收藏時間

`MovieRepositoryImpl.insertMovieCollect()` 將在寫入前以注入的時間來源覆寫輸入 movie 的 `timestamp`；正式注入使用 Kotlin Multiplatform 可用的時鐘，測試注入固定時間。如此不論收藏操作起自 Android Compose 或 SwiftUI，資料都由同一層建立，UI 不必理解持久化欄位。

替代方案是在每個 UI 點擊 callback 填入時間。這會重複跨平台邏輯，且目前 iOS 已遺漏，因此不採用。替代方案為 DAO 使用 insertion rowid 排序，但資料表以電影 id 為主鍵且採 `REPLACE`，不能明確表達收藏時間，也不採用。

### 由 `MovieCollectDao` 宣告排序契約

`getAllMovies()` 的 SQL 將以 `timestamp DESC` 回傳資料，必要時以 id 作為確定性的次要排序。Repository 只維持 entity-to-model mapping；Android 與 iOS 收藏頁都直接渲染這個 shared 結果。

替代方案在兩端 ViewModel 或 View 以 `sortedByDescending` 處理。該方案會讓資料契約分裂、容易遺漏新的 consumer，且不能處理 iOS 寫入零時間戳的根因，故不採用。

### iOS 標頭沿用 HistoryView 模式

FavoritesView 會使用與 `HistoryView` 相同的 `Text`、字級、水平 padding 與 divider 組成標頭，字串新增至 `Localizable.xcstrings`。標頭固定顯示於空狀態與清單狀態，讓收藏 tab 有明確頁面識別。

替代方案使用 `.navigationTitle`。現有 HistoryView 採內容內標頭，為了視覺一致性與避免導航欄樣式差異，不採用。

## Risks / Trade-offs

- [既有資料的 timestamp 可能全為 0] → 不做 schema migration；它們會依次要排序呈現，使用者後續重新收藏時會取得正確新時間。
- [注入時間來源改變 repository 建構式] → 由既有 Koin module 提供正式時間來源，並調整 repository 測試 builder 注入固定值，避免不穩定測試。
- [Room KMP 的 SQL 跨平台一致性] → 以共用 DAO query 實作，並在 Android host 與 iOS simulator DAO 測試驗證。
- [iOS 本地化 key 遺漏語系值] → 更新既有 String Catalog 並以 Swift 測試或 UI 結構測試確認標頭 key 被使用。

## Migration Plan

1. 先加入 repository 時間來源與 DAO 排序，再補 shared 測試。
2. 調整 iOS 收藏頁標頭與測試；Android 不需 UI 邏輯變更，將以 shared 結果驗證其順序來源。
3. 不提升資料庫版本、不執行 migration；若需回退，移除 `ORDER BY` 與時間覆寫即可，既有資料不受破壞。

## Open Questions

無；本次排序固定為最新收藏優先，且由 shared 資料層統一提供。
