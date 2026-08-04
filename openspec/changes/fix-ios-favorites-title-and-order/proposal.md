## Why

iOS 收藏頁缺少與既有歷史頁一致的頁面標題；收藏清單也沒有保證以最新收藏優先顯示。Android 雖已有標題，但同樣直接使用未排序的 shared 查詢結果，因此兩個平台的顯示順序都取決於 SQLite 未保證的自然順序。

此外，收藏時間目前由 Android `MovieCard` 寫入，而 iOS 傳入預設值 `0`。排序規則若只加在 DAO，iOS 新增的收藏仍無法正確比較先後。

## What Changes

- 在 shared 收藏寫入路徑建立收藏當下的時間戳，讓 Android 與 iOS 都使用同一份可排序資料；取消收藏不改變既有行為。
- 將收藏清單的排序收斂至 `MovieCollectDao`，以收藏時間由新到舊回傳，供 Android 與 iOS 共用。
- 補上 iOS 收藏頁的在地化標題，版面與 iOS 歷史頁標頭一致。
- 補齊 shared DAO／repository 與 iOS 收藏頁的測試，驗證跨平台的最新收藏排序與標題呈現。

## Capabilities

### New Capabilities

無。

### Modified Capabilities

- `kmp-movie-local-database`: 收藏 DAO 回傳清單的順序必須依收藏時間由新到舊。
- `kmp-movie-data-repository`: 新增收藏時必須由 shared 層記錄收藏當下時間，並對外提供已排序清單。
- `android-collect-module`: Android 收藏頁必須顯示 shared 提供的最新收藏優先順序。
- `ios-movie-collection`: iOS 收藏頁必須顯示在地化標題，且以最新收藏優先順序顯示。

## Impact

- 受影響模組：`shared/model`、`shared/data`、`shared/database`、`feature/collect`、`iosApp`。
- 受影響 API：`MovieRepository.insertMovieCollect()` 的時間戳來源改由 shared 寫入層決定；公開方法簽名不變。
- 受影響資料：既有 `MovieCollectEntity.timestamp` 欄位沿用，不需 schema migration；舊資料的同時間戳排序將以資料庫次序作為次要結果。
- 不新增 Gradle 或 Swift Package 依賴。
