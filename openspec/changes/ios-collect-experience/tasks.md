## 1. shared/app 跨平台資料入口驗證

- [ ] 1.1 確認 Swift 可透過既有 `KoinHelper.getMovieRepository()` 以 SKIE 呼叫 `getAllMovieCollect()`、`insertMovieCollect()` 與 `deleteMovieCollect()`，並記錄實際匯出的 Swift API 形式。
- [ ] 1.2 僅在 1.1 證實既有 API 無法安全消費時，於 `shared/app` 新增最小的具名 iOS bridge（不得洩漏 DAO／Room 型別），並新增或調整對應 Kotlin 測試。

## 2. iosApp 收藏資料與操作層

- [ ] 2.1 在 `iosApp/iosApp/Favorites/` 建立可建構子注入 `MovieRepository` 的 `FavoritesViewModel`，將收藏 Flow 轉為 SwiftUI 可觀察的 loading／empty／content state，並在生命週期結束時停止觀察 Task。
- [ ] 2.2 在 iOS 可重用的收藏操作位置實作加入／移除切換：依 `MovieCardData.movieCardIsCollect` 呼叫 shared Repository，並讓 shared Flow 成為畫面最終狀態來源。
- [ ] 2.3 新增 `FavoritesViewModel` 與收藏切換操作的 Swift 單元測試，涵蓋既有收藏載入、空清單、加入收藏及移除收藏；測試採 AAA 並以 fake／protocol 隔離 shared 依賴。

## 3. iosApp 收藏畫面與導覽組裝

- [ ] 3.1 將 `FavoritesView` placeholder 改為收藏畫面：有資料時以既有 `MovieCardView` + `LazyVGrid` 顯示，無資料時顯示在地化空狀態。
- [ ] 3.2 由 `MainTab`／組裝根取得 shared Repository 並注入 `FavoritesViewModel`，不得在 View 或卡片內直接呼叫 `KoinHelper`。
- [ ] 3.3 在 `Localizable.xcstrings` 新增或更新收藏頁標題、空狀態及必要操作文案的繁中與英文翻譯。
- [ ] 3.4 為收藏畫面補 SwiftUI Preview，覆蓋至少一個有資料與一個空狀態的替身資料情境。

## 4. iosApp 首頁收藏接線

- [ ] 4.1 將 `HomeContentView` 建構所需的收藏操作依賴由 `HomeView`／`HomeViewModel` 向下傳遞，並把 `MovieCardView.onCollectTap` 接到切換操作。
- [ ] 4.2 確認首頁收藏狀態更新依現有 `GetHomeMovieListUseCase` 與 `HomeMovieListPresenter` emission 重新渲染；若 snapshot 未更新，僅在 `shared/app` iOS Presenter 補最小的更新通知。
- [ ] 4.3 為首頁 ViewModel／收藏 callback 接線補 Swift 單元測試，驗證按下未收藏與已收藏卡片分別呼叫正確 shared 操作。
- [ ] 4.4 為 `MovieCardView` 補測試或可驗證的 UI 測試，確認點擊愛心只執行 `onCollectTap`，不會同時觸發 `onMovieTap`。

## 5. 驗證與人工 iOS 驗收

- [ ] 5.1 執行 `./gradlew ktlintCheck`，並在有修改 shared Kotlin 時執行受影響 shared 模組的測試。
- [ ] 5.2 在 macOS 執行 `./gradlew iosFormatCheck iosLint`；若本機缺少 Swift 工具，記錄清楚錯誤並改以 Xcode formatter／lint 流程驗證。
- [ ] 5.3 由你在 Xcode 手動驗收：首頁加入收藏、收藏 tab 即時出現；收藏頁取消後首頁愛心同步變空；取消最後一筆顯示空狀態；重啟 app 後收藏仍存在。
