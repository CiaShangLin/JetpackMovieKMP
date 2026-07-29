## 1. shared/app：iOS Koin bridge

- [ ] 1.1 在 `shared/app/src/iosMain/.../KoinHelper.kt` 新增 `getHistoryMovieListUseCase()` 具名 accessor，從既有 Koin `domainModule()` 解析 `GetHistoryMovieListUseCase`。
- [ ] 1.2 在 `shared/app/src/iosTest/.../KoinHelperTest.kt` 啟動 iOS Koin 後驗證新 accessor 可解析非 null UseCase，並執行對應 iOS shared 測試。

## 2. iosApp：依賴組裝、History presentation 與導覽整合

- [ ] 2.1 建立單一 `AppDependencies` SwiftUI environment；由 `IosApp` 在 Koin 初始化後組裝 shared 依賴，並改除 `MainView`／`MainTab` 對 `MovieRepository` 的逐 tab 轉送。
- [ ] 2.2 建立 `HistoryUiState`，將空陣列與非空歷史電影清單映射為 `.empty`／`.success(data:)`。
- [ ] 2.3 建立 `@MainActor @Observable` 的 `HistoryViewModel`：以 `GetHistoryMovieListUseCase` 監聽 SKIE 匯出的 Flow、用 `MovieCollectAction` 切換收藏、呼叫 `deleteAllMovieHistory()` 清空，並防止同類寫入操作重複觸發。
- [ ] 2.4 以 `HistoryView` 取代 placeholder：從 `AppDependencies` 取得所需依賴並以建構子建立 ViewModel；有資料時顯示標題、清空按鈕與重用 `MovieCardView` 的自適應 `LazyVGrid`；空資料時顯示歷史專用的空狀態；所有資料更新以 shared Flow emission 為準。
- [ ] 2.5 將 history tab 改接 `HistoryView`；新增 feature 依賴時不增加 `MainView`／`MainTab` 的轉送參數，且 View／ViewModel 不得直接呼叫 `KoinHelper`。
- [ ] 2.6 更新 `Localizable.xcstrings` 的英語與繁中 history 標題、清空、空狀態文案，移除未使用的 `main_history_placeholder`；Xcode 使用檔案系統同步群組，不手動新增 `.pbxproj` file reference。

## 3. iosAppTests：History 單元測試

- [ ] 3.1 新增 `HistoryUiState` XCTest，以 AAA 驗證空歷史映射為 `.empty`、非空資料映射為 `.success(data:)`。
- [ ] 3.2 新增 History 操作／ViewModel XCTest（以可替換的測試邊界或 shared repository test double）驗證未收藏與已收藏電影分別發出新增／刪除收藏，以及清空進行中第二次請求不會再次呼叫 repository。
- [ ] 3.3 確認 `MovieCardView` 的收藏 callback 不會移除歷史項目，並在 shared Flow 發出更新後驗證 UI state 反映最新收藏狀態與清空後空狀態。

## 4. 驗證

- [ ] 4.1 執行 `./gradlew :shared:app:iosSimulatorArm64Test`，確認 Koin bridge 與既有 shared iOS 測試通過。
- [ ] 4.2 以 `xcodebuild -list -project iosApp/iosApp.xcodeproj` 取得實際 scheme 後，執行 iOS Simulator 的 build 與 `iosAppTests` XCTest。
- [ ] 4.3 執行 `./gradlew iosFormatCheck iosLint`；如本機工具缺失，明確記錄缺少 `swiftformat` 或 `swiftlint`，並在具備工具的 macOS 環境補跑。
