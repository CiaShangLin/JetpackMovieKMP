## Context

`MainActivity.kt` 的 `SuccessScreen()` 使用單一共用 `NavBackStack<NavKey>`（`rememberNavBackStack(HomeKey)`）同時承載「底部導覽 Tab 切換」與「電影詳情頁 push/pop」兩種導覽行為。底部導覽的 5 個 Tab（`MainNavItem`：HOME／COLLECT／SEARCH／HISTORY／SETTING）沒有各自獨立的 back stack，全部共用同一份 `backStack`。

目前 Tab 切換的 `onClick`：

```kotlin
onClick = {
    if (currentKey != item.key) {
        backStack.removeLastOrNull()
        backStack.add(item.key)
    }
},
```

每次切換都是「移除目前最後一筆、加入新 Tab key」，backStack 的長度恆為 1。`androidx.navigation3.ui.NavDisplay` 內建的 predictive back 攔截只在 `backStack.size > 1` 時對 `OnBackPressedDispatcher` 啟用回呼；backStack 恆為 1 筆導致該回呼永遠停用，返回鍵事件直接交給系統預設行為（結束 `MainActivity`）。這與詳情頁的 push/pop（`backStack.add(MovieDetailKey(...))` / `backStack.removeLastOrNull()`）邏輯彼此獨立、互不影響，因為底部導覽只在 `currentKey !is MovieDetailKey` 時顯示（見 `SuccessScreen()` 的 `if (currentKey is MovieDetailKey) { navDisplay(); return }` 分支）。

## Goals / Non-Goals

**Goals:**
- 使用者切換底部 Tab 後按返回鍵，MUST 依「造訪順序」回到前一個造訪過的 Tab，而非直接退出 App。
- 重複切換同一組 Tab（例如 Home ↔ Collect 來回切換多次）時，backStack 長度 MUST 有上限（不隨切換次數無限增長），且不得出現同一 Tab key 的重複項目。
- 完全不改動詳情頁（`MovieDetailKey`）既有 push/pop 行為與 `android-app-entry` spec 中「巢狀 detail 返回」相關 Scenario。
- 不引入自訂 `BackHandler` / `OnBackPressedDispatcher`，維持完全依賴 `NavDisplay` 內建的返回鍵處理機制（含 predictive back 動畫）。

**Non-Goals:**
- 不實作「再按一次返回退出 App」的 Toast/Snackbar 提示（使用者只要求依 Tab 造訪順序返回，退出時機仍是「backStack 只剩 1 筆再按返回」）。
- 不將 5 個 Tab 改為 Nav3 官方 multi-back-stack 模式（每個 Tab 各自獨立 backStack，保留 Tab 內部畫面堆疊）。目前 Tab 內部本來就沒有巢狀導覽（除了共用的 detail push），維持單一 backStack architecture 即可滿足本次需求，範圍更小、風險更低。
- 不改變 Tab 初始狀態（App 啟動時 backStack 仍是 `[HomeKey]` 一筆，若使用者從未切換過 Tab，返回鍵行為維持現狀：直接退出）。

## Decisions

### 決策 1：Tab 切換改用 MRU（Most-Recently-Used）reordered back stack，取代 replace-last

將 `onClick` 邏輯改為：

```kotlin
onClick = {
    if (currentKey != item.key) {
        backStack.remove(item.key)
        backStack.add(item.key)
    }
},
```

即「若目前點擊的 Tab key 已存在於 backStack 中任何位置，先移除該筆舊項目，再重新加入尾端（成為目前項目）；若不存在則直接加入尾端」。

**為什麼選這個方案而非其他兩個曾考慮的方向：**
- **方案 A（本次採用）：MRU reordered back stack。** 這是 Android 官方對「底部導覽 + 單一 back stack」情境的建議模式（Now in Android 範例、Material Design 導覽指南皆採此模式），改動範圍最小（只動 `onClick` 一處邏輯），且 backStack 長度自然收斂在「Tab 總數上限」（本專案 5 個），不需額外的長度控管邏輯。
- **方案 B（未採用）：純 push，不做 dedup。** 直接 `backStack.add(item.key)` 不移除舊項目。使用者若在 5 個 Tab 間反覆切換多次，backStack 會無限增長（例如來回切 20 次就有 20 筆），不僅記憶體/狀態成本隨切換次數增加，也會讓返回鍵行為出現大量「切回同一個 Tab」的多餘步驟，體驗上不直覺，故排除。
- **方案 C（未採用）：每個 Tab 各自維護獨立 backStack（官方 multi-back-stack pattern）。** 能達到「Tab 內部畫面堆疊也各自保留」的更完整體驗，但本專案目前 Tab 內部沒有巢狀導覽需求（只有共用的 detail push，且 detail 開啟時整個 Navigation Suite 會隱藏），引入 5 份獨立 backStack 加上「如何決定目前作用中 backStack」的協調邏輯，複雜度與本次「修正返回鍵直接退出」的問題规模不成比例，屬於過度設計，故排除；若未來 Tab 內部需要各自巢狀導覽再視需求評估。

**與 detail push 邏輯的邊界：** `MovieDetailKey` 的 push（`backStack.add(MovieDetailKey(...))`）與 pop（`backStack.removeLastOrNull()`）維持不變，不套用 dedup 邏輯——`backStack.remove(item.key)` 只在 Tab 切換的 `onClick` 內對 `MainNavItem` 的 `NavKey` 生效，不影響 `MovieDetailKey`。

### 決策 2：不新增 `BackHandler`，維持完全依賴 `NavDisplay` 內建返回鍵處理

延續現有架構（`NavDisplay(backStack = backStack, onBack = { backStack.removeLastOrNull() }, ...)`），只調整 backStack 的內容管理方式，不額外攔截返回鍵事件。好處是行為與 `NavDisplay` 內建的 predictive back 動畫、系統手勢完全一致，不需要自行維護 `enabled` 條件判斷或處理 predictive back 的過場動畫細節。

## Risks / Trade-offs

- **[風險] `NavBackStack.remove(element)` 需確認 API 是否存在／簽名是否符合預期** → 緩解：`NavBackStack` 為 `SnapshotStateList<NavKey>` 的包裝（Navigation3 1.1.4），標準 `MutableList.remove(element: T): Boolean` 語意可用；若實測發現該 API 不可用或行為與預期不符，退回以 `indexOf` + `removeAt` 手動實作等價邏輯。
- **[風險] `backStack.remove(item.key)` 在項目不存在時回傳 `false` 但不拋例外，需確認不會影響後續 `add`** → 緩解：`remove` 找不到元素時為 no-op，後續 `add(item.key)` 正常執行，等同「該 Tab 是第一次造訪，直接加入」，符合預期。
- **[取捨] 使用者連續切換全部 5 個 Tab 後，backStack 會有 5 筆，需要按 5 次返回鍵才會退出 App** → 這是「依 Tab 造訪順序逐一返回」需求下的預期行為（使用者本次明確選擇此行為），非缺陷；若日後有「回到首頁才允許退出」的獨立需求，屬於另一個提案範圍。
- **[取捨] 不處理「初次啟動、未曾切換 Tab」情境下的返回鍵行為** → 維持現狀（size 為 1，直接退出），與本次修正範圍（Tab 切換後的返回行為）一致，不擴大變更範圍。

## Migration Plan

不涉及資料庫 schema、DataStore 或跨模組 API 變更，屬於單一 UI 邏輯調整，無需 migration 或 rollback 特殊流程；有問題時直接還原 `MainActivity.kt` 該處 `onClick` 邏輯即可。
