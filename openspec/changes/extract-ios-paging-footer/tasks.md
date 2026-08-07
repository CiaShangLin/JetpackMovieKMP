## 1. iosApp：新增共用 Paging Footer 元件

- [ ] 1.1 新增 `iosApp/iosApp/Common/Paging/AppendLoadState.swift`：定義純 Swift `enum AppendLoadState { case idle, loading, error(message: String) }`
- [ ] 1.2 新增 `iosApp/iosApp/Common/Paging/PagingAppendFooterView.swift`：接受 `AppendLoadState` 與 `onRetry: () -> Void`，依 idle/loading/error 渲染既有樣式（`EmptyView` / `ProgressView().padding()` / 重試 `Button(...).padding()`）
- [ ] 1.3 新增 `paging_append_retry_button` localization key 至 `Localizable.xcstrings`，文字內容與既有 `home_retry_button` 一致

## 2. iosApp：Home 改用共用元件

- [ ] 2.1 搜尋整個 iosApp 是否有其他呼叫端引用 `home_retry_button`，確認除 Home／Search 外無其他使用者
- [ ] 2.2 在 `HomeContentView.swift` 新增 `HomeMovieListLoadState → AppendLoadState` 的映射邏輯
- [ ] 2.3 移除 `HomeContentView.swift` 本地的 `appendFooter` computed property，改為呼叫 `PagingAppendFooterView`（`onRetry` 綁定既有 `viewModel.retry()`）
- [ ] 2.4 確認 Footer padding 改為共用元件內建值（四邊 8pt），移除 Home 原本 `.padding(.horizontal, JMSpacing.spacing16)` 的 Footer 專屬 padding

## 3. iosApp：Search 改用共用元件

- [ ] 3.1 在 `SearchView.swift` 新增 `SearchMovieListLoadState → AppendLoadState` 的映射邏輯
- [ ] 3.2 移除 `SearchView.swift` 本地的 `appendFooter` computed property，改為呼叫 `PagingAppendFooterView`（`onRetry` 綁定既有 `viewModel.retry()`）
- [ ] 3.3 `SearchView.swift` 改用新的 `paging_append_retry_button` key（取代原本借用的 `home_retry_button`）
- [ ] 3.4 確認 Footer padding 與共用元件內建值一致（四邊 8pt，維持原有數值不變）

## 4. iosApp：測試

- [ ] 4.1 新增 `iosApp/iosAppTests/Common/Paging/AppendLoadStateMappingTests.swift`（或依既有測試資料夾慣例命名），涵蓋 `HomeMovieListLoadState`／`SearchMovieListLoadState` 各三態映射到 `AppendLoadState` 的正確性
- [ ] 4.2 確認既有 `SearchViewRenderingConditionsTests.swift`／`SearchViewModelTests.swift` 在改動後仍全數通過，必要時補上針對 retry callback 呼叫共用元件 `onRetry` 的驗證
- [ ] 4.3 於 macOS 執行 iOS Simulator 手動驗證：Home 與 Search 的載入更多 loading／error／retry 三態，以及下拉刷新後 Footer 行為與改動前一致

## 5. 跨模組驗證

- [ ] 5.1 執行 `./gradlew iosFormat` 與 `./gradlew iosFormatCheck`
- [ ] 5.2 執行 `./gradlew iosLint` 與 `./gradlew iosCodeStyleCheck`
- [ ] 5.3 於 Xcode 執行 `iosAppTests`（含新增與既有 Search／Favorites 測試）
- [ ] 5.4 執行 `openspec validate extract-ios-paging-footer --type change --strict --no-interactive`
