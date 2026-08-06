## 1. core/ui

- [ ] 1.1 修改 `core/ui/src/main/kotlin/com/shang/jetpackmoviekmp/core/ui/MovieCard.kt` 的 `MovieTitle` composable，為 `Text` 加上 `minLines = 2`（與既有 `maxLines = 2` 並存）。
- [ ] 1.2 確認 `MovieTitle` 未被其他檔案以不同參數重複定義或覆寫（避免遺漏修改點）。

## 2. 驗證

- [ ] 2.1 `MovieTitle` 為無狀態 Composable，不涉及 ViewModel 或狀態邏輯，`core/ui` 亦無既有的 `MovieCard`/`MovieTitle` UI 測試或 screenshot 測試基礎設施，故不新增自動化測試；改以下列手動驗證取代，並於 PR 說明中記錄結果：
  - [ ] 2.1.1 執行 `./gradlew :androidApp:assembleDebug`（Windows：`.\gradlew.bat :androidApp:assembleDebug`）確認建置成功。
  - [ ] 2.1.2 於首頁 Grid、搜尋結果、收藏清單、瀏覽歷史畫面中，分別找一筆標題 1 行與標題 2 行以上的電影資料，確認兩張卡片的標題區塊與整體卡片高度一致，且短標題卡片標題下方出現對齊 iOS 的留白。
  - [ ] 2.1.3 確認標題超過 2 行的電影資料仍正確截斷為 2 行，未撐高卡片。
- [ ] 2.2 執行 `./gradlew ktlintCheck`（Windows：`.\gradlew.bat ktlintCheck`）確認格式檢查通過。

## 3. 最終驗證

- [ ] 3.1 執行 `openspec validate android-movie-card-title-two-lines --type change --strict --no-interactive` 確認 change 文件通過驗證。
- [ ] 3.2 執行 `./gradlew check`（Windows：`.\gradlew.bat check`）完整驗證（含 ktlint）。
