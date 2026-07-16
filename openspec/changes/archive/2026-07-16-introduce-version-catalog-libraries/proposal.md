## Why

目前 `gradle/libs.versions.toml` 已建立多數 KMP 與 Android library alias，但 build scripts 尚未依實際用途逐步引入，後續功能開發仍缺少可編譯、可啟動驗證過的依賴基線。
本 change 要把 catalog 內 library 依 KMP 優先、Android 次之的順序逐一接入專案，降低一次導入大量依賴造成的定位成本。

## What Changes

- 依 `libs.versions.toml` 盤點尚未被 build scripts 使用的 library alias。
- 優先引入支援 Kotlin Multiplatform 的 library 到 `shared` 對應 source set。
- 將 Paging3 的 KMP/common 依賴納入 `shared.commonMain`，Android UI paging 依賴留在 `androidApp`。
- 將 Room KMP runtime、paging integration、bundled SQLite、Room plugin 與 KSP compiler target wiring 納入本次依賴基線。
- Android-only library 只引入 `androidApp` 或 Android source set，不污染 `commonMain`。
- iOS-only library 預設不主動引入，除非該 KMP library 必須配置 iOS platform dependency 才能完成共同契約。
- 每引入一個 library 或一組不可拆分的同生態依賴後，立即執行 Gradle 編譯驗證。
- 每次 Gradle 驗證通過後，使用 `android-cli` 安裝並啟動 Android app 做 smoke test。
- 若單一 library 引入失敗，最多嘗試 3 次修復；仍失敗時記錄原因並跳過，繼續下一個 library。
- 不在此 change 新增實際電影功能或重構既有 UI，只建立 dependency adoption baseline。

## Capabilities

### New Capabilities

- None.

### Modified Capabilities

- `kmp-dependency-catalog`: 擴充 catalog 依賴從「已定義 alias」到「按 KMP/Android 邊界逐步引入並驗證」的需求。

## Impact

- 受影響 module：
  - `shared`: KMP library 會依支援範圍加入 `commonMain`、`commonTest`、`androidMain` 或 iOS source set。
  - `androidApp`: Android-only library 與 Android app smoke test 會在此模組驗證。
  - `gradle/libs.versions.toml`: 作為依賴來源；原則上不新增版本，除非實作時發現缺少必要 alias。
- 受影響 build files：
  - `shared/build.gradle.kts`
  - `androidApp/build.gradle.kts`
  - `shared/schemas/`：Room schema directory 的受版控占位目錄。
  - root `build.gradle.kts` 或 `settings.gradle.kts` 僅在 plugin/application wiring 必要時調整。
- 本專案沒有 `buildSrc` 的 `DependenciesVersions`、`Dependencies`、`DependenciesProvider`；新增依賴將直接使用 Gradle Version Catalog alias。
- 驗證系統：
  - Gradle Android debug compile/assemble。
  - KMP shared host tests 或 metadata compile。
  - `android-cli` 裝置/模擬器安裝與啟動 smoke test。
