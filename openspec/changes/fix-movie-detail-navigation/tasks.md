## 1. androidApp — 修復電影詳情導覽空白畫面

- [ ] 1.1 在 `MainActivity.kt` 的 `mainEntry()` 的 `when` 分支加入 `is MovieDetailKey -> movieDetailEntry(key = navKey, onBackClick = { backStack.removeLastOrNull() }, onMovieClick = { movie -> backStack.add(MovieDetailKey(movie.movieCardId)) }).second`
- [ ] 1.2 確認 `homeEntry` / `collectEntry` / `historyEntry` / `searchEntry` 呼叫端傳入的 `onMovieClick` 仍正確把 `MovieDetailKey(movieId)` 加入 `backStack`（本次未變更，僅需確認未受影響）
- [ ] 1.3 確認 `feature/detail` 的 `movieDetailEntry()` 內建的推薦電影卡點擊會把新的 `MovieDetailKey` 疊加到 back stack 最後方，並僅移除最後一個 `MovieDetailKey` 即可正確返回

## 2. androidApp — 隱藏主 Navigation Suite

- [ ] 2.1 在 `SuccessScreen()` 依 `currentKey is MovieDetailKey` 條件式包裝 `JMNavigationSuiteScaffold`：為 `MovieDetailKey` 時直接渲染 `NavDisplay`，其餘 root destination 維持既有 `JMNavigationSuiteScaffold` 包裝
- [ ] 2.2 確認從 detail 返回 Home／Search／Collect／History 後，`JMNavigationSuiteScaffold` 與底部導覽選中狀態恢復正確

## 3. androidApp — 清除死程式碼

- [ ] 3.1 移除 `MainActivity.kt` 中未被任何呼叫端使用的 `DetailNavDisplay()` Composable
- [ ] 3.2 全 repo 搜尋確認移除後無編譯錯誤、無殘留呼叫點

## 4. 驗證

- [ ] 4.1 執行 `./gradlew ktlintFormat ktlintCheck` 確認格式與風格通過
- [ ] 4.2 執行 `./gradlew :androidApp:assembleDebug` 確認可成功建置
- [ ] 4.3 在裝置／模擬器上手動驗證：分別從首頁、搜尋、收藏、觀看紀錄點擊電影卡，確認皆正確導向電影詳情頁（不再是空白畫面）；在 detail 頁點擊推薦電影卡可繼續巢狀導覽；使用返回鍵可逐層返回並在回到 root 頁面時底部導覽正確顯示且選中狀態正確
- [ ] 4.4 執行 `openspec archive fix-movie-detail-navigation`（待使用者確認實作完成後）將本次 change 歸檔並同步 `openspec/specs/android-app-entry/spec.md`
