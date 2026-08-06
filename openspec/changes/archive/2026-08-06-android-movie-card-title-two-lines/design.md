## Context

Android 電影卡片元件 `MovieTitle`（`core/ui/src/main/kotlin/com/shang/jetpackmoviekmp/core/ui/MovieCard.kt:108-117`）目前只設定 `maxLines = 2`，未設定 `minLines`。Compose `Text` 在未設定 `minLines` 時，區塊高度會依實際換行結果變動：標題 1 行時只佔 1 行高度，標題 2 行時佔 2 行高度。`MovieTitle` 外層 `Column` 與卡片 `Box` 皆未使用固定高度手段，因此標題行數會直接反映到卡片整體高度。

iOS 端 `MovieCardView.titleSection`（`iosApp/iosApp/Common/MovieCard/MovieCardView.swift:71-78`）已使用 `.lineLimit(2, reservesSpace: true)`，讓標題文字區塊不論實際行數皆保留 2 行垂直空間，卡片高度因此穩定。此變更僅涉及 `core/ui` 單一 Compose 元件的樣式參數調整，不涉及 Repository、UseCase、Koin module 或跨模組依賴方向。

## Goals / Non-Goals

**Goals:**
- 讓 Android `MovieTitle` 標題區塊不論標題實際行數，皆固定佔用 2 行垂直空間，對齊 iOS `reservesSpace: true` 的行為。
- 消除因標題行數不同造成的卡片高度參差問題。

**Non-Goals:**
- 不調整標題以外的樣式（字型大小、顏色、padding、`titleMedium` typography）。
- 不調整卡片其他區塊（海報比例、評分徽章、收藏按鈕、上映日期）的版面。
- 不新增可設定的行數參數或抽出可重用 Composable 抽象——僅有此單一使用情境，暫不需要泛化。
- 不修改 iOS 端程式碼（iOS 已符合預期行為，僅作為對齊基準）。

## Decisions

### 使用 `minLines = 2` 搭配既有 `maxLines = 2`，不使用固定高度容器

**選擇**：在 `MovieTitle` 的 `Text` 加上 `minLines = 2`：

```kotlin
Text(
    text = title,
    style = MaterialTheme.typography.titleMedium,
    color = MaterialTheme.colorScheme.onSurface,
    modifier = Modifier.padding(start = 8.dp, top = 8.dp, end = 8.dp),
    minLines = 2,
    maxLines = 2,
)
```

**理由**：
- `minLines`/`maxLines` 是 Compose `Text` 原生支援的行數語意 API（對應 iOS `lineLimit(2, reservesSpace: true)` 的效果），由 Compose 依 `titleMedium` 的 lineHeight 自行計算所需高度，不需手動換算 dp 數值。
- 相較於用 `Modifier.height(<固定 dp>)` 或 `heightIn(min = <固定 dp>)` 撐版，`minLines` 不會寫死與字型大小綁定的魔術數字；若日後 `titleMedium` 的字級或行高調整，高度會自動跟著重新計算，不需同步修改硬編碼的 dp 值。
- 改動範圍最小，只修改 1 個 composable 的 2 個參數，不影響外層 `Column`/`Box` 佈局結構。

**考慮過的替代方案**：
1. **`Modifier.heightIn(min = 40.dp)`（或其他固定 dp）加在 `Text` 或外層容器**：需手動估算 2 行 `titleMedium` 文字所需高度，字級或 lineHeight 調整時容易與實際文字行為脫節，故不採用。
2. **在 `MovieCard` 外層用固定高度 `Box` 包住整個標題區**：影響範圍擴大到佈局結構而非樣式參數，且仍需硬編碼高度數值，違反最小改動原則，故不採用。

## Risks / Trade-offs

- **[視覺變更]** 標題只有 1 行的電影卡片，標題下方會多出約 1 行高度的空白，與變更前的緊湊排版不同 → **緩解**：此為使用者已確認的預期效果，且與 iOS 現有行為一致，屬刻意的跨平台對齊，非缺陷。
- **[全域生效範圍]** `MovieTitle` 為 `core/ui` 共用元件，改動會同時影響首頁 Grid、搜尋、收藏、歷史等所有使用 `MovieCard` 的畫面 → **緩解**：這些畫面共用同一元件本為既有架構，使用者已確認接受全域生效範圍；不需要也不應該為個別畫面加上例外開關。
- **[UI 測試覆蓋]** `core/ui` 目前若無既有的 `MovieCard`/`MovieTitle` UI 測試或 screenshot 測試，此變更缺乏自動化回歸保護 → **緩解**：於 tasks.md 中列出以手動或既有測試流程確認短標題與長標題兩種情境的顯示結果。

## Migration Plan

不涉及資料遷移。純 UI 樣式調整，屬單一 commit 即可完成的變更，無需分階段部署或 feature flag。若發現視覺回歸，直接還原 `minLines = 2` 參數即可回滾，無額外相依步驟。
