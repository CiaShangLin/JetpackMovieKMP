## 1. shared/app：iOS Koin bridge

- [x] 1.1 在 `shared/app/src/iosMain/.../KoinHelper.kt` 新增 `getHistoryMovieListUseCase()` 具名 accessor，從既有 Koin `domainModule()` 解析 `GetHistoryMovieListUseCase`。
- [x] 1.2 在 `shared/app/src/iosTest/.../KoinHelperTest.kt` 啟動 iOS Koin 後驗證新 accessor 可解析非 null UseCase，並執行對應 iOS shared 測試。

## 2. iosApp：直接 Koin 注入、History presentation 與導覽整合

- [x] 2.1 讓需要 shared 依賴的 SwiftUI View 直接透過 `KoinHelper` 建立 ViewModel，並改除 `MainView`／`MainTab` 對 `MovieRepository` 的逐 tab 轉送。
- [x] 2.2 建立 `HistoryUiState`，將空陣列與非空歷史電影清單映射為 `.empty`／`.success(data:)`。
- [x] 2.3 建立 `@MainActor @Observable` 的 `HistoryViewModel`：以 `GetHistoryMovieListUseCase` 監聽 SKIE 匯出的 Flow、用 `MovieCollectAction` 切換收藏、呼叫 `deleteAllMovieHistory()` 清空，並防止同類寫入操作重複觸發。
- [x] 2.4 以 `HistoryView` 取代 placeholder：直接透過 `KoinHelper` 取得所需依賴並建立 ViewModel；有資料時顯示標題、清空按鈕與重用 `MovieCardView` 的自適應 `LazyVGrid`；空資料時顯示歷史專用的空狀態；所有資料更新以 shared Flow emission 為準。
- [x] 2.5 將 history tab 改接 `HistoryView`；新增 feature 依賴時不增加 `MainView`／`MainTab` 的轉送參數，並允許 feature View 直接呼叫 `KoinHelper`。
- [x] 2.6 更新 `Localizable.xcstrings` 的英語與繁中 history 標題、清空、空狀態文案，移除未使用的 `main_history_placeholder`；Xcode 使用檔案系統同步群組，不手動新增 `.pbxproj` file reference。

## 3. 驗證

- [x] 3.1 執行 `./gradlew :shared:app:iosSimulatorArm64Test`，確認既有 shared iOS 測試通過。
- [x] 3.2 以 `xcodebuild -list -project iosApp/iosApp.xcodeproj` 取得實際 scheme 後，執行 iOS Simulator build。
- [x] 3.3 執行 `./gradlew iosFormatCheck iosLint`；如本機工具缺失，明確記錄缺少 `swiftformat` 或 `swiftlint`，並在具備工具的 macOS 環境補跑。
