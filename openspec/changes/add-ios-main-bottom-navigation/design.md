## Context

iOS App 目前由 `iOSApp` 初始化 Koin，先顯示 `SplashView` 載入 TMDB configuration，成功後切換到 `MainView`。`MainView` 目前仍是暫時性文字畫面；Android 端的主畫面已規劃底部導覽項目，但實際功能也仍以首頁與 placeholder 架構為主。

本次目標是先建立 iOS 首頁導覽骨架，讓後續各頁功能可以依固定 tab 入口增量導入。

## Goals / Non-Goals

**Goals:**

- 使用 SwiftUI 原生 `TabView` 建立 iOS bottom navigation。
- 提供五個 tab：首頁、收藏、搜尋、歷史、設定。
- 每個 tab 第一版顯示 placeholder text。
- 導覽標題與 placeholder 文案透過 `Localizable.xcstrings` 管理。
- 保持變更集中於 `iosApp`。

**Non-Goals:**

- 不在本次串接首頁電影列表。
- 不在本次實作收藏、搜尋、歷史、設定的實際功能。
- 不新增共用層 use case、repository 或 Koin accessor。
- 不自訂 bottom navigation 外觀，不重建 Android 的 Material NavigationSuite。

## Decisions

### 使用 SwiftUI 原生 TabView

採用 `TabView` 作為主畫面的底部導覽容器。這符合 iOS 使用者對底部 tab bar 的平台預期，也不需要額外依賴或自訂 layout。

替代方案是自訂 bottom nav 元件以更貼近 Android 外觀，但本階段需求是建立 iOS 首頁骨架，原生 `TabView` 的可維護性與平台一致性更適合。

### 建立 iOS 專用 MainTab 模型

以 Swift enum 管理五個 tab 的識別、標題 key、SF Symbol 與 placeholder key，避免 tab 設定散落在 `MainView` 多處。此模型只屬於 iOS UI 層，不放入 shared，因為目前沒有跨平台共用 UI 狀態的需求。

### MainViewModel 第一版保持輕量

本次遵循既有 SwiftUI / MVVM 方向，但不引入 Repository / Use Case，因為 bottom navigation 第一版只處理 UI tab selection 與 placeholder 顯示。Splash 已經負責 configuration loading，Main 不重複載入與錯誤重試流程。

若實作時需要持有目前選取 tab，可由 `MainView` 使用 `@State` 管理，或由 `MainViewModel` 暴露 selection；選擇以最少狀態滿足 UI 測試與可讀性。

### Placeholder 不代表功能完成

五個 tab 都先顯示可辨識的 placeholder text，作為導覽驗收目標。後續首頁電影列表、收藏清單、搜尋、歷史與設定功能應各自另開 change 實作。

## Risks / Trade-offs

- Placeholder 可能被誤認為功能完成 -> 文案與 tasks 需明確標示本次只完成導覽入口，不交付實際頁面資料。
- SwiftUI `TabView` 預設樣式受 iOS 系統版本影響 -> 驗收以五個 tab 可見、可切換、文案正確為主，不鎖定像素級外觀。
- iOS UI 缺少現成單元測試框架覆蓋 `TabView` 互動 -> 本次以 Swift 原始碼結構檢查、String Catalog 檢查與可編譯驗證為主要驗收方式。
- 本次不涉及資料庫 schema 變更 -> 不需要 Room migration 策略。

## Migration Plan

1. 在 `iosApp` 新增或調整 Main tab 結構。
2. 補齊 `Localizable.xcstrings` 的五個 tab 與 placeholder 文案。
3. 驗證 iOS App 可編譯，並確認 `SplashView` 成功後仍切換到 `MainView`。

Rollback 時可移除本次新增的 tab model / placeholder view，將 `MainView` 還原為單一內容畫面；不涉及資料或 schema 回復。

## Open Questions

- 無。使用者已確認第五個 tab 名稱為「設定」、使用原生 bottom navigation、頁面內容先放 placeholder text。
