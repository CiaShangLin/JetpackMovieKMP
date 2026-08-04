## 1. shared/database

- [ ] 1.1 將 `MovieCollectDao.getAllMovies()` 改為以 `timestamp DESC` 與電影 id 的確定性次要排序回傳收藏資料，不變更 Room schema。
- [ ] 1.2 擴充 `MovieCollectDao` 的 iOS simulator 測試，驗證不同收藏時間與相同收藏時間的排序結果。

## 2. shared/data

- [ ] 2.1 在 `MovieRepositoryImpl` 注入可測試的跨平台時間來源，並於 `insertMovieCollect()` 寫入收藏當下時間後再轉為 entity。
- [ ] 2.2 更新 `dataModule()` 提供正式時間來源，維持 `MovieRepository` 公開 API 與既有 Koin 組裝方式。
- [ ] 2.3 擴充 `MovieRepositoryImplTest`，驗證 iOS/Android 呼叫端傳入的既有 timestamp 不會決定持久化收藏時間，且 `getAllMovieCollect()` 保留 DAO 的最新優先順序；維持 Kover 80% 覆蓋率。

## 3. feature/collect

- [ ] 3.1 確認 Android `CollectViewModel`／`CollectScreen` 直接保留 shared repository emission 的順序，不新增 UI 層排序。
- [ ] 3.2 補充或調整 `CollectViewModelTest`，驗證最新收藏優先的輸入順序會原樣交給成功 UI state。

## 4. iosApp

- [ ] 4.1 在 `Localizable.xcstrings` 新增收藏頁標題的在地化字串。
- [ ] 4.2 依 `HistoryView` 的既有 header 模式，在 `FavoritesView` 的空狀態與清單上方顯示收藏頁標題與分隔線，並直接渲染 shared 的排序結果。
- [ ] 4.3 新增或調整 Swift 單元測試，驗證收藏標題的在地化 key 與 Favorites UI 結構；保留既有收藏 Flow 更新與取消收藏行為測試。

## 5. 驗證

- [ ] 5.1 執行 shared database/data 的 Android host 與 iOS simulator 相關測試，確認 Room KMP 的收藏排序一致。
- [ ] 5.2 執行 `./gradlew :shared:data:koverVerify :shared:network:koverVerify` 與 `./gradlew ktlintCheck`。
- [ ] 5.3 執行 iOS Swift 測試、`./gradlew iosFormatCheck` 與 `./gradlew iosLint`，確認標題與 Swift 格式規範。
