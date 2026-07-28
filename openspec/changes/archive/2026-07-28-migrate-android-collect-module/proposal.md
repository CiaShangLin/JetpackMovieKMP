## Why

目前 Android App 的底部導覽僅能進入首頁，既有的本地收藏資料能力雖已完成，使用者卻無法從 Android UI 檢視或管理收藏電影。遷移來源專案的 collect feature，可補齊 Android 端收藏瀏覽流程，並沿用 KMP 專案既有的 Room、Repository、Koin 與 Navigation3 架構。

## What Changes

- 新增 Android-only `feature:collect` Gradle 模組，承載收藏清單的 ViewModel、Compose 畫面、Navigation3 entry 與 Koin module。
- 顯示 `MovieRepository.getAllMovieCollect()` 的即時收藏清單；無收藏時顯示空狀態。
- 讓使用者在收藏清單中點擊收藏按鈕後移除該電影，並即時更新清單與空狀態。
- 將 collect 分頁接入 `MainNavItem`、`MainActivity` 的 Navigation3 `NavDisplay`，並載入 collect Koin module。
- 加入收藏頁所需的 Android 字串與空狀態圖片資源，並補齊 ViewModel 行為測試。

## Capabilities

### New Capabilities

- `android-collect-module`: Android 使用者可由底部導覽進入收藏頁，檢視並移除本機收藏的電影。

### Modified Capabilities

- `android-app-entry`: Android 主導覽新增收藏目的地，並將其分派至已遷移的 collect feature。

## Impact

- 受影響模組：`feature/collect`（新增）、`androidApp`、`shared/data`（僅使用既有 `MovieRepository` 介面）、`shared/model`（僅使用既有 `MovieCardData`／`MovieCardResult`）、`core/ui`、`core/designsystem`。
- `settings.gradle.kts` 與 `androidApp/build.gradle.kts` 需接入新 feature module；預期所有 Gradle 依賴皆沿用既有 `gradle/libs.versions.toml` alias，不新增版本或外部依賴。
- 不變更資料庫 schema、Repository API 或 iOS App；本次只處理 Android collect UI 與導覽整合。
