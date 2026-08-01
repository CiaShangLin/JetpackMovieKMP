## Context

`MainActivity.kt` 目前的導覽骨架如下：

- `SuccessScreen()` 實際使用的 `NavDisplay` 之 `entryProvider = { navKey -> mainEntry(navKey, backStack) }`。
- `mainEntry()` 的 `when` 只涵蓋 `HomeKey / CollectKey / HistoryKey / SearchKey / SettingKey`，其餘（含 `MovieDetailKey`）一律落入 `else -> NavEntry(navKey) { PlaceholderScreen() }`（空的 `Box`）。
- `homeEntry` / `collectEntry` / `historyEntry` / `searchEntry` 傳入的 `onMovieClick` 都已正確把 `MovieDetailKey(movieId)` 加入 `backStack`，代表「點擊卡片→導覽動作」本身沒有問題，問題出在 `NavDisplay` 拿到 `MovieDetailKey` 後無法對應到真正的 `movieDetailEntry()`。
- Repo 內另存在 `DetailNavDisplay()`，內部 `when` 正確處理了 `is MovieDetailKey -> movieDetailEntry(...)`，但此函式在整個 repo 中沒有任何呼叫點，是 `feat(detail)`／`fix(android): restore merged feature imports` 兩次合併留下的死程式碼。
- `android-app-entry` spec 已明確要求「Android App MUST 支援電影詳情 Navigation3 目的地」與「detail 顯示時 MUST 隱藏主 Navigation Suite」；後者目前也未實作——`SuccessScreen` 永遠用 `JMNavigationSuiteScaffold` 包住 `NavDisplay`，未依 `currentKey` 是否為 `MovieDetailKey` 切換。

## Goals / Non-Goals

**Goals:**

- 讓 `mainEntry()` 正確處理 `MovieDetailKey`，使 `NavDisplay` 渲染 `feature:detail` 的 `movieDetailEntry()`，修復點擊電影卡後的空白畫面。
- 依既有 spec 要求，在 `MovieDetailKey` 為 back stack 最後一項時隱藏 `JMNavigationSuiteScaffold`，返回後恢復。
- 移除 `DetailNavDisplay()` 這段未被呼叫的死程式碼，避免未來維護時誤判「已經接好了」。

**Non-Goals:**

- 不重新設計 Navigation3 骨架或改變 `NavKey` 的資料結構。
- 不新增依賴、不變更 `feature:detail` 模組對外介面（`MovieDetailKey` / `movieDetailEntry()` 簽章維持不變）。
- 不處理與本 bug 無關的其他導覽項（例如 Setting 的巢狀導覽）。

## Decisions

1. **在 `mainEntry()` 直接加入 `is MovieDetailKey -> movieDetailEntry(...)` 分支**，而非新增另一個獨立的 entryProvider。
   - 原因：`SuccessScreen` 只呼叫 `mainEntry()`，所有既有呼叫端（Home/Search/Collect/History/detail 內推薦電影卡）都已把 `MovieDetailKey` 加進同一個 `backStack`，讓同一個 `entryProvider` 統一處理最符合現有 Navigation3 typed `NavKey` 模式，改動範圍最小。
   - 考慮過的替代方案：讓 `SuccessScreen` 改用 `DetailNavDisplay()`。但 `DetailNavDisplay()` 的邏輯只是「`MovieDetailKey` 特判 + else 委派回 `mainEntry()`」，與直接把該 case 併入 `mainEntry()` 效果相同，卻多一層間接、且是本次要清掉的舊死程式碼，故不採用。

2. **移除 `DetailNavDisplay()`**。
   - 原因：加入上述分支後，`DetailNavDisplay()` 與 `mainEntry()` 邏輯完全重疊，且已透過調查（Explore 代理 + repo 內 grep）確認沒有任何呼叫點；保留只會增加未來誤用或誤判「detail 已串接」的風險，符合「移除範圍聚焦、不留半成品」原則。

3. **`SuccessScreen` 依 `currentKey is MovieDetailKey` 條件式包裝 `JMNavigationSuiteScaffold`**：
   ```kotlin
   if (currentKey is MovieDetailKey) {
       NavDisplay(backStack = backStack, onBack = { backStack.removeLastOrNull() }, entryProvider = { navKey -> mainEntry(navKey, backStack) })
   } else {
       JMNavigationSuiteScaffold(...) { NavDisplay(...) }
   }
   ```
   - 原因：不需改動 `core/designsystem` 的 `JMNavigationSuiteScaffold` 公開 API（例如加 `visible` 參數），維持模組邊界最小改動；`NavDisplay` 的狀態綁定在 `backStack`，脫離／重新進入 Scaffold 不影響 back stack 內容。
   - 考慮過的替代方案：讓 `JMNavigationSuiteScaffold` 內部接受 `visible: Boolean` 參數自行決定是否渲染 navigation suite。因涉及 `core/designsystem` 公開元件簽章變更、影響面較廣，且本次修復目的是讓現有行為符合既有 spec，故不採用，留作未來如有其他頁面也需隱藏主導覽時再評估。

4. 全程沿用既有 MVVM／Repository／UseCase／Navigation3 typed `NavKey` 模式，未偏離既有架構慣例。

## Risks / Trade-offs

- [Risk] 移除 `DetailNavDisplay()` 可能誤刪未來要用的程式碼 → Mitigation：已用 Explore 代理與 grep 確認全 repo 無呼叫點，PR review 階段再次確認 diff 中沒有遺漏的呼叫端。
- [Risk] `SuccessScreen` 改為條件式包裝 `JMNavigationSuiteScaffold` 可能造成切換時底部導覽選中狀態或畫面短暫閃爍 → Mitigation：`currentKey` 的選中狀態計算方式不變，僅決定是否包一層 Scaffold；合併後以 `assembleDebug` + 手動 QA（進入 detail → 返回 → 確認底部導覽選中狀態與捲動位置正確）驗證。
- 不涉及資料庫 schema 變更，無需 Room migration。

## Migration Plan

純 UI／導覽程式碼修正，不涉及資料遷移、不影響既有使用者資料，無需 feature flag 或分階段上線。標準流程：實作 → `ktlintCheck` → `assembleDebug` → 手動 QA（見上）→ PR review → 合併。若合併後發現迴歸，直接 revert 該 commit 即可，無額外 rollback 步驟。
