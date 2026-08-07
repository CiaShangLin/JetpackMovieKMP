## 1. iosApp：新增共用 `MovieCollectToggler`

- [x] 1.1 新增 `iosApp/iosApp/Common/MovieCollectToggler.swift`：從 `SearchViewModel.swift` 搬移 `MovieCollectionToggling` protocol 與 `MovieRepositoryCollectionAdapter`（保留原有 doc comment）
- [x] 1.2 於同一檔案新增 `@MainActor final class MovieCollectToggler`：持有 `private let repository: MovieCollectionToggling`、`private var isUpdatingCollection = false`，建構子接收 `MovieCollectionToggling`（預設值 `MovieRepositoryCollectionAdapter(repository: KoinHelper.shared.getMovieRepository())`）
- [x] 1.3 實作 `func toggle(currentIsCollect: Bool, movie: MovieCardResult) async`：guard/defer 防連點 + `if currentIsCollect { delete } else { insert }` + `catch { print("切換收藏失敗：\(error.localizedDescription)") }`
- [x] 1.4 從 `SearchViewModel.swift` 移除已搬移的 `MovieCollectionToggling`／`MovieRepositoryCollectionAdapter` 定義，確認 `iosApp` target 仍可編譯（尚待 Xcode 實機編譯驗證，見 5.3）

## 2. iosApp：各 ViewModel 改用 `MovieCollectToggler`

- [x] 2.1 `FavoritesViewModel.swift`：新增 `MovieCollectToggler` 屬性（建構子注入，預設值同 1.2），`toggleMovieCollectStatus(data: MovieCardData)` 改為 `await toggler.toggle(currentIsCollect: data.movieCardIsCollect, movie: data.asMovieCardResult())`；移除本檔案原有的 `MovieCollectAction` enum 定義（暫不刪除，待 3.1 測試搬移完成後於任務 5 統一移除）
- [x] 2.2 `HomeContentViewModel.swift`：同 2.1 模式改用 `MovieCollectToggler`
- [x] 2.3 `SearchViewModel.swift`：改為持有 `MovieCollectToggler`（取代直接持有 `MovieCollectionToggling`），`toggleMovieCollectStatus(data:)` 改為委派呼叫；確認既有建構子注入慣例（測試可注入 Fake）維持不變
- [x] 2.4 `HistoryViewModel.swift`：改用 `MovieCollectToggler`，同時將方法 `toggleMovieCollect(data:)` 改名為 `toggleMovieCollectStatus(data:)`
- [x] 2.5 `HistoryView.swift`：同步更新第 99 行呼叫端方法名稱為 `toggleMovieCollectStatus`
- [x] 2.6 `MovieDetailViewModel.swift`：新增 `MovieCollectToggler` 屬性；`toggleMovieCollectStatus(data: MovieCardResult)` 改為 `await toggler.toggle(currentIsCollect: isCollect, movie: data)`；`toggleRecommendCollectStatus(data: MovieCardData)` 改為 `await toggler.toggle(currentIsCollect: data.movieCardIsCollect, movie: data.asMovieCardResult())`；`observeCollectStatus()` 與 `isCollect` 屬性維持原樣不動

## 3. iosApp：測試搬移與補強

- [x] 3.1 將 `iosApp/iosAppTests/Search/FakeMovieCollectionToggling.swift` 搬移至 `iosApp/iosAppTests/Common/FakeMovieCollectionToggling.swift`，確認 `SearchViewModelTests.swift` 的 import／參照路徑更新後仍可編譯
- [x] 3.2 新增 `iosApp/iosAppTests/Common/MovieCollectTogglerTests.swift`：覆蓋 insert 分支（`currentIsCollect: false`）、delete 分支（`currentIsCollect: true`）、連續呼叫時第二次被防連點擋下（可用 `FakeMovieCollectionToggling.beforeInsert` 製造時間點）、repository 拋錯時不 crash 且不向上傳播
- [x] 3.3 刪除 `iosApp/iosAppTests/Favorites/MovieCollectActionTests.swift`（案例已改寫進 3.2）
- [x] 3.4 為 `FavoritesViewModel` 補上收藏切換測試（若無既有 `FavoritesViewModelTests.swift` 則新增），驗證呼叫 `toggler.toggle` 時傳入正確的 `currentIsCollect`／`movie`
- [x] 3.5 為 `HomeContentViewModel` 補上收藏切換測試（若無既有測試檔則新增），同 3.4 驗證方式
- [x] 3.6 為 `HistoryViewModel` 補上收藏切換測試（若無既有測試檔則新增），並驗證改名後的 `toggleMovieCollectStatus` 呼叫端行為不變
- [x] 3.7 為 `MovieDetailViewModel` 補上收藏切換測試（若無既有測試檔則新增），分別驗證 `toggleMovieCollectStatus`（依 `isCollect` 屬性判斷）與 `toggleRecommendCollectStatus`（依 `MovieCardData.movieCardIsCollect` 判斷）兩條路徑

## 4. 清理

- [x] 4.1 移除 `FavoritesViewModel.swift` 中已不再被任何呼叫端使用的 `MovieCollectAction` enum
- [x] 4.2 全專案搜尋 `MovieCollectAction` 確認無殘留引用

## 5. 跨模組驗證

- [x] 5.1 執行 `./gradlew iosFormat` 與 `./gradlew iosFormatCheck`（驗收時重跑：先前記錄的 androidApp 簽章問題已不再阻擋，Gradle wrapper 可正常執行，0 violations）
- [x] 5.2 執行 `./gradlew iosLint` 與 `./gradlew iosCodeStyleCheck`（同上，可正常執行，0 violations）
- [x] 5.3 於 Xcode 執行 `iosAppTests`（含新增的 `MovieCollectTogglerTests`、各 ViewModel 收藏切換測試，以及既有 `SearchViewModelTests`）全數通過；驗收時發現並修正一個真實編譯錯誤：`MovieCollectToggler.init(repository:)` 原為 `@MainActor`-isolated，因 Swift 預設參數值一律在非隔離上下文求值，導致 `FavoritesViewModel`／`HistoryViewModel` 的 `init(toggler: MovieCollectToggler = MovieCollectToggler())` 編譯失敗；修法為將該 init 標記 `nonisolated`。另補上 `MovieDetailViewModelTests` 缺少的 `toggleMovieCollectStatus` delete 分支測試。
- [x] 5.4 於 iOS Simulator 手動驗證 Favorites／Home／Search／History／Detail 五處收藏切換行為與改動前一致（含 Detail 頁面收藏狀態顯示與推薦清單收藏切換；使用者已於 Simulator 手動確認）
- [x] 5.5 執行 `openspec validate refactor-ios-favorite-toggle --type change --strict --no-interactive`
