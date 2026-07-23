## Why

`iosApp` 為原生 SwiftUI，目前消費 `shared:app` 匯出的 Kotlin `suspend` function 與
`Flow`（例如 5 個 UseCase 回傳的 `Flow<Result<T>>`、`Flow<PagingData<T>>`）僅能透過
Kotlin/Native 預設的 Objective-C 橋接介面操作，缺乏原生 async/await、AsyncSequence
語法，Swift 端互通體感差。導入 SKIE（Touchlab 出品的 Kotlin 編譯器 plugin）可讓
`suspend` function 自動對應 Swift async/await、`Flow` 自動對應 `AsyncSequence`、
sealed class 對應 Swift enum，且不需手動撰寫 wrapper 或加 annotation。

## What Changes

- 在 `shared/app`（產出 iOS `Shared.framework` 的唯一模組）套用 SKIE Gradle plugin，
  版本對應本專案 Kotlin 2.4.0（SKIE 0.10.13 起支援 Kotlin 2.4.0）。
- 於 `gradle/libs.versions.toml` 新增 SKIE 的 plugin id 與 version alias，比照既有
  version catalog 慣例集中管理版本號，不在 `shared/app/build.gradle.kts` 內硬編版本。
- 不修改任何既有 UseCase / Repository 的 public 簽名；SKIE 是編譯期橋接層增強，
  對 `shared:common`、`shared:model`、`shared:data`、`shared:domain` 既有匯出 API
  是否需要調整（例如簡化 `Flow<Result<T>>` 的回傳型態）留待導入後依實測結果評估，
  不在本次變更範圍內。
- **不**同時導入 Kotlin 2.4.0 原生的 Swift Export（Alpha 階段、與 Objective-C export
  互斥、且會與現有 `iosTarget.binaries.framework` + Objective-C 橋接架構衝突），本次
  變更維持現有 Objective-C export 路徑，僅疊加 SKIE 做橋接體驗增強。

## Capabilities

### New Capabilities
- `ios-skie-interop`: 定義 SKIE 在 `shared/app` 的套用方式、對 Swift 端 async/await
  與 AsyncSequence 互通的行為約束，以及已知限制（自訂例外無法跨界傳遞、`Flow<Result<T>>`
  等泛型型別行為）的因應方式。

### Modified Capabilities
- `kmp-dependency-catalog`: 新增 SKIE plugin id／version alias 至 `gradle/libs.versions.toml`
  的治理範圍。

## Impact

- **`gradle/libs.versions.toml`**：新增 `skie` version 與對應 plugin alias（不新增 library
  alias，SKIE 是純 Gradle plugin，不需額外 runtime dependency）。
- **`shared/app/build.gradle.kts`**：套用 SKIE plugin（唯一套用點，因為這是唯一宣告
  `iosTarget.binaries.framework` 的模組；SKIE 會對此 framework 匯出的所有依賴模組
  程式碼生效，涵蓋 `shared:common`、`shared:model`、`shared:data`、`shared:domain`）。
- **建置流程**：`./gradlew :shared:app:assembleSharedReleaseXCFramework`（或既有 iOS
  framework 建置 task）需額外編譯 SKIE 產生的 Swift 橋接層，預期會增加建置時間，
  但不影響 `androidApp` 建置與 `:shared:data`／`:shared:network` 的 `testAndroidHostTest`、
  `koverVerify`（SKIE 只作用於 iOS binary 輸出）。
- **iOS 測試**：`iosSimulatorArm64Test` 不受 SKIE 影響（SKIE 只轉換對外 Objective-C/Swift
  介面，不改變 Kotlin/Native 內部測試執行方式）。
- **不影響** `androidApp`、`core/designsystem`、`core/ui`（純 Android 模組，未匯出至
  iOS framework）。
- **與既有 backlog 項目的關係**：`ios-koin-bridge` 現有的 `KoinHelper` 具名橋接物件
  不需修改簽名，但 SKIE 上線後，Swift 端呼叫 `KoinHelper.shared.xxx()` 取得的 UseCase
  執行 `Flow`／`suspend` 結果時，語法會從 Objective-C 風格 callback／`Kotlinx_coroutines_coreFlow`
  改為原生 `AsyncSequence`／`async throws`，屬於呼叫端體驗改善，非橋接介面本身變更。
