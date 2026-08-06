## 1. feature/detail

- [x] 1.1 在 `MovieDetailScreen.kt` 的 `MovieDetailContent` 中，於 `LazyColumn` 加上 `contentPadding = WindowInsets.navigationBars.asPaddingValues()`，取代目前完全沒有 `contentPadding` 的狀態。
- [x] 1.2 確認未對 `LazyColumn` 的 parent container（`Column`/`Box`）額外加上底部 `Modifier.padding()`，避免與 `contentPadding` 重複或裁切內容。
- [x] 1.3 手動驗證：在有導覽列（三鍵導覽列與手勢導覽列）的 Android 裝置或模擬器上，開啟一個推薦電影數量足以觸底的電影詳情頁，確認最後一排卡片完整可見且可點擊。

## 2. 最終驗證

- [x] 2.1 執行 `./gradlew :androidApp:assembleDebug` 確認可正常建置。
- [x] 2.2 執行 `./gradlew ktlintCheck` 確認格式通過。
- [x] 2.3 執行 `openspec validate fix-detail-recommend-bottom-inset --type change --strict --no-interactive` 確認 change 文件通過驗證。
