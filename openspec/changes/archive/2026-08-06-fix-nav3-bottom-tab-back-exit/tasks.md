## 1. androidApp：抽出可測試的 MRU back stack 邏輯

- [x] 1.1 在 `androidApp/src/main/kotlin/com/shang/jetpackmoviekmp/ui/MainActivity.kt`（或同套件新檔案，例如 `MainNavBackStack.kt`）新增一個不依賴 Compose runtime 的純函式，例如 `internal fun switchTab(backStack: MutableList<NavKey>, target: NavKey)`：若 `target` 已存在於 `backStack` 中先移除舊項目，再 `add` 到尾端；否則直接 `add`。簽名使用 `MutableList<NavKey>` 而非 `NavBackStack<NavKey>`，以便在 JVM 單元測試中用一般 `mutableListOf<NavKey>()` 驗證，不需啟動 Compose 測試環境。
- [x] 1.2 將 `SuccessScreen()` 內 `JMNavigationSuiteScaffold` 的 `onClick`（目前為 `backStack.removeLastOrNull()` + `backStack.add(item.key)`）改為呼叫 1.1 新增的 `switchTab(backStack, item.key)`。
- [x] 1.3 確認 `MovieDetailKey` 的 push（`backStack.add(MovieDetailKey(...))`）與 pop（`backStack.removeLastOrNull()`）邏輯未被本次改動影響（`mainEntry()`、`movieDetailEntry` 呼叫端維持原樣）。

## 2. androidApp：單元測試

- [x] 2.1 在 `androidApp/src/test/kotlin/com/shang/jetpackmoviekmp/ui/`（新檔案，例如 `MainNavBackStackTest.kt`）以 AAA 模式為 `switchTab` 撰寫測試，至少涵蓋：
  - 依序切換 Home → Collect → Search，backStack 依序累積為 `[HomeKey, CollectKey, SearchKey]`
  - 已存在於 backStack 中的 Tab 被再次點擊時，移除舊位置並移到尾端（例如 `[HomeKey, CollectKey]` 點擊 Home 後變為 `[CollectKey, HomeKey]`），且不產生重複項目
  - 點擊目前已在頂端的 Tab（`target == backStack.last()`）不重複加入（維持現有 `if (currentKey != item.key)` 呼叫端判斷下的等價行為）
- [x] 2.2 執行 `./gradlew :androidApp:testDebugUnitTest --tests "com.shang.jetpackmoviekmp.ui.MainNavBackStackTest"` 確認新測試通過。

## 3. 手動驗證（Android 裝置／模擬器）

- [x] 3.1 啟動 App，依序點擊底部導覽列 Home → Collect → Search，確認畫面正確切換。
- [x] 3.2 從 Search 頁連續按下系統返回鍵，確認依序回到 Collect 頁、Home 頁，最後一次按下才結束 App（對應 `openspec/changes/fix-nav3-bottom-tab-back-exit/specs/android-app-entry/spec.md` 的「返回鍵依 Tab 造訪順序逐一返回」情境）。
- [x] 3.3 重複點擊同一個 Tab（例如連續點擊 Home 兩次、或在 `[HomeKey, CollectKey]` 狀態下再點 Home），確認未出現返回鍵需要「多按一次同 Tab」的異常步驟。
- [x] 3.4 從任一 Tab 進入電影詳情頁後按返回鍵，確認回到進入前的 Tab，行為與變更前一致（未受本次修改影響）。
- [x] 3.5 App 啟動後不切換任何 Tab，直接在首頁按一次返回鍵，確認 App 直接結束（維持既有行為）。

## 4. 最終驗證

- [x] 4.1 執行 `./gradlew ktlintCheck` 確認格式檢查通過。
- [x] 4.2 執行 `./gradlew :androidApp:assembleDebug` 確認可正常建置。
- [x] 4.3 執行 `openspec validate fix-nav3-bottom-tab-back-exit --type change --strict --no-interactive` 確認 change 產物通過驗證。
