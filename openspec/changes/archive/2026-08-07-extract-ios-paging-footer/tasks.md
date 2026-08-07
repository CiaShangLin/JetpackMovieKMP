## 1. iosApp：新增共用 Paging Footer 元件

- [x] 1.1 新增 `iosApp/iosApp/Common/Paging/AppendLoadState.swift`：定義純 Swift `enum AppendLoadState { case idle, loading, error(message: String) }`
- [x] 1.2 新增 `iosApp/iosApp/Common/Paging/PagingAppendFooterView.swift`：接受 `AppendLoadState` 與 `onRetry: () -> Void`，依 idle/loading/error 渲染（`EmptyView` / `ProgressView().padding(JMSpacing.spacing8)` / 重試 `Button(...).padding(JMSpacing.spacing8)`）
- [x] 1.3 新增 `paging_append_retry_button` localization key 至 `Localizable.xcstrings`，文字內容與既有 `home_retry_button` 一致

## 2. iosApp：Home 改用共用元件

- [x] 2.1 搜尋整個 iosApp 是否有其他呼叫端引用 `home_retry_button`，確認除 Home／Search 外無其他使用者
- [x] 2.2 在 `HomeContentView.swift` 新增 `HomeMovieListLoadState → AppendLoadState` 的映射邏輯
- [x] 2.3 移除 `HomeContentView.swift` 本地的 `appendFooter` computed property，改為呼叫 `PagingAppendFooterView`（`onRetry` 綁定既有 `viewModel.retry()`）
- [x] 2.4（已修正範圍，詳見 design.md 附註）Home 的 `.padding(.horizontal, JMSpacing.spacing16)` 實為 LazyVGrid 卡片格線的左右邊界，非 Footer 專屬，維持不變；Footer 自身的 8pt padding 已內建於共用元件 `PagingAppendFooterView`

## 3. iosApp：Search 改用共用元件

- [x] 3.1 在 `SearchView.swift` 新增 `SearchMovieListLoadState → AppendLoadState` 的映射邏輯
- [x] 3.2 移除 `SearchView.swift` 本地的 `appendFooter` computed property，改為呼叫 `PagingAppendFooterView`（`onRetry` 綁定既有 `viewModel.retry()`）
- [x] 3.3 `SearchView.swift` 改用新的 `paging_append_retry_button` key（取代原本借用的 `home_retry_button`）
- [x] 3.4 確認 Footer padding 與共用元件內建值一致（四邊 8pt，維持原有數值不變）

## 4. iosApp：測試

- [x] 4.1 新增 `iosApp/iosAppTests/Common/Paging/AppendLoadStateMappingTests.swift`（或依既有測試資料夾慣例命名），涵蓋 `HomeMovieListLoadState`／`SearchMovieListLoadState` 各三態映射到 `AppendLoadState` 的正確性
- [x] 4.2 確認既有 `SearchViewRenderingConditionsTests.swift`／`SearchViewModelTests.swift` 在改動後仍全數通過（`xcodebuild test` 全綠，見 5.3）；retry callback 綁定是 View 層一行 `onRetry: { viewModel.retry() }`，與改動前的寫法同構，無額外可測邏輯，不補測試
- [x] 4.3 於 macOS 執行 iOS Simulator 手動驗證：Home 與 Search 的載入更多 loading／error／retry 三態，以及下拉刷新後 Footer 行為與改動前一致（使用者已於 Simulator 手動確認）

## 5. 跨模組驗證

- [x] 5.1 執行 `./gradlew iosFormat` 與 `./gradlew iosFormatCheck`
- [x] 5.2 執行 `./gradlew iosLint` 與 `./gradlew iosCodeStyleCheck`
- [x] 5.3 於 Xcode 執行 `iosAppTests`（含新增與既有 Search／Favorites 測試，`** TEST SUCCEEDED **`）
- [x] 5.4 執行 `openspec validate extract-ios-paging-footer --type change --strict --no-interactive`（`Change 'extract-ios-paging-footer' is valid`）
