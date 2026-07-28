## MODIFIED Requirements

### Requirement: `MainActivity` 主要導覽骨架 MUST 使用 Navigation3

`MainActivity` 的主要導覽骨架 MUST 使用專案既定的 `androidx.navigation3:navigation3-runtime` 與 `androidx.navigation3:navigation3-ui`（`NavBackStack`／`NavDisplay`／entry provider），MUST NOT 依賴 classic `androidx.navigation:navigation-compose`（`NavHostController`／`NavHost`／`rememberNavController`）。已遷移的首頁與收藏頁 MUST 各自使用 typed `NavKey`，並由 `MainNavItem` 驅動底部導覽切換。

#### Scenario: 不存在 classic Navigation Compose 依賴

- **WHEN** 檢查 `androidApp/build.gradle.kts` 與 `MainActivity.kt` 的 import
- **THEN** MUST NOT 出現 `androidx.navigation:navigation-compose` 依賴或 `androidx.navigation.NavHostController`／`androidx.navigation.compose.NavHost`／`androidx.navigation.compose.rememberNavController` 的 import

#### Scenario: 使用 Navigation3 API 建立導覽骨架

- **WHEN** 檢查 `MainActivity.kt` 的導覽相關實作
- **THEN** MUST 使用 `rememberNavBackStack()` 建立 backstack 並交給 `NavDisplay` 渲染
- **AND** `MainNavItem` 驅動的導覽列點擊事件 MUST 透過操作該 backstack（而非字串路由 `navigate()`）切換畫面

#### Scenario: 導覽列可進入收藏頁

- **WHEN** 使用者點擊底部導覽列的收藏項目
- **THEN** `MainNavItem.COLLECT` 對應的 `CollectKey` MUST 成為目前 backstack 項目
- **AND** `NavDisplay` 的 entryProvider MUST 回傳渲染 `CollectScreen` 的 collect `NavEntry`，不得回退為 `PlaceholderScreen`
