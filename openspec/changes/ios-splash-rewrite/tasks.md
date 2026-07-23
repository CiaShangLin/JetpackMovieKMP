## 1. shared/app（KoinHelper accessor）

- [ ] 1.1 在 `shared/app` iosMain 的 `KoinHelper.kt` 新增 `getConfigurationUseCase(): GetConfigurationUseCase = getKoin().get()`
- [ ] 1.2 執行既有 iOS framework 建置指令，確認可正常產出且無新增建置錯誤

## 2. 設計決議確認（已於討論階段完成，實作前快速覆核）

- [x] 2.1 進場動畫效果：Logo 淡入 + 輕微縮放（見 design.md Decision）
- [x] 2.2 `SplashViewModel` 風格：`@Observable` macro（見 design.md Decision 3）
- [x] 2.3 `KoinHelper.shared.getConfigurationUseCase()` 呼叫位置：`SplashView` 屬性初始化處（見 design.md Decision 4）
- [x] 2.4 Splash 完成後導轉：成功切換至 `MainView`；失敗顯示錯誤文案 + 重試按鈕（見 design.md Decision 5、6）

## 3. iosApp/Splash 實作（使用者親自撰寫，Claude 逐步討論與輔助，不由 Claude 代寫）

- [ ] 3.1 依討論結論調整 `SplashUiState.swift`（若骨架已足夠則略過）
- [ ] 3.2 實作 `SplashViewModel.swift`：`@Observable` + `@MainActor`，建構子接收 `GetConfigurationUseCase`，呼叫並以 `for await` 迭代、用 `as? ConfigurationBean` 判斷成功/失敗，映射到 `SplashUiState`；失敗時提供重新觸發拿取的方法供重試按鈕呼叫
- [ ] 3.3 實作 `SplashView.swift`：`@State private var viewModel = SplashViewModel(getConfigurationUseCase: KoinHelper.shared.getConfigurationUseCase())`；加入 Logo 淡入 + 縮放進場動畫；依 `SplashUiState` 顯示 loading／success／failure（含重試按鈕）對應內容；用 `.task { }` 啟動非同步拿取流程
- [ ] 3.4 調整 `iosApp/iosApp/iOSApp.swift`：加入 root 狀態切換，`SplashUiState` 成功後從 `SplashView` 切換到 `MainView`

## 4. Review 與驗證

- [ ] 4.1 Claude review 使用者完成的 Swift 實作，確認遵循 `ios-koin-bridge` 慣例（組裝根取得實例、`SplashViewModel` 內部不得直接呼叫 `KoinHelper`）與 `@Observable` + `@State` 搭配是否正確
- [ ] 4.2 於模擬器手動執行，確認成功與失敗（例如離線）兩種路徑皆能正確顯示對應 `SplashUiState`，重試按鈕可正常重新觸發拿取，且不會 runtime crash
- [ ] 4.3 執行 `./gradlew ktlintCheck`，確認 `KoinHelper.kt` 修改通過格式檢查
- [ ] 4.4 確認 `openspec/changes/ios-splash-rewrite/specs/` 內容與實際實作一致；若討論過程中方案有調整，回填 `design.md` 的 Open Questions 結論
