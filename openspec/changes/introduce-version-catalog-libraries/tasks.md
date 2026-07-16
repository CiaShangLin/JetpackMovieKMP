## 1. shared dependency inventory

- [ ] 1.1 盤點 `gradle/libs.versions.toml` 中尚未被 `shared/build.gradle.kts` 或 `androidApp/build.gradle.kts` 使用的 library alias，建立本次導入順序紀錄。
- [ ] 1.2 將 alias 分類為 `commonMain/commonTest` 可用、需要 platform source set 分流、Android-only、iOS-only 或暫不導入。
- [ ] 1.3 確認每個待導入 alias 是否可單獨加入；若必須成組導入，記錄最小不可拆分 group 與原因。

## 2. shared commonMain KMP libraries

- [ ] 2.1 將 `kotlinx-coroutines-core` 加入 `shared` 的 `commonMain`，執行 Gradle 驗證，成功後用 `android-cli` 啟動 app。
- [ ] 2.2 將 `kotlinx-serialization-json` 加入 `shared` 的 `commonMain`，必要時套用 serialization plugin，執行 Gradle 驗證，成功後用 `android-cli` 啟動 app。
- [ ] 2.3 將 Ktor common aliases 依序加入 `shared` 的 `commonMain`：`ktor-client-core`、`ktor-client-content-negotiation`、`ktor-serialization-kotlinx-json`、`ktor-client-logging`；每個 alias 或必要最小 group 加入後都執行 Gradle 驗證與 `android-cli` app 啟動。
- [ ] 2.4 將 `koin-core` 加入 `shared` 的 `commonMain`，執行 Gradle 驗證，成功後用 `android-cli` 啟動 app。
- [ ] 2.5 評估 `androidx-datastore`、`androidx-datastore-preferences`、`androidx-paging-common`、`coil-network-ktor3` 是否可在目前 `shared` source set 無產品功能前安全加入；可加入者逐一導入並完成 Gradle 與 `android-cli` 驗證，不可加入者記錄跳過原因。

## 3. shared platform source set libraries

- [ ] 3.1 將 `ktor-client-cio` 加入 `shared` 的 Android source set，執行 Gradle 驗證，成功後用 `android-cli` 啟動 app。
- [ ] 3.2 僅在 Ktor client common setup 需要 iOS engine 才將 `ktor-client-darwin` 加入 iOS source set；若本輪不需要，記錄為 iOS-only skipped。
- [ ] 3.3 評估 `androidx-room-runtime`、`androidx-room-compiler`、`androidx-room-paging`、`androidx-sqlite-bundled` 與 Room/KSP plugins 的最小 wiring；若未建立 database layer 就無法安全導入，記錄跳過原因，否則依 platform target 逐項導入並完成驗證。

## 4. shared commonTest and host test libraries

- [ ] 4.1 將 `kotlinx-coroutines-test` 加入 `shared` 的 `commonTest`，執行 `:shared:testAndroidHostTest` 或最接近可用的 shared test task。
- [ ] 4.2 將 `ktor-client-mock` 加入 `shared` 的 `commonTest`，執行 shared test Gradle 驗證。
- [ ] 4.3 將 `koin-test` 加入 `shared` 的 `commonTest`，執行 shared test Gradle 驗證。
- [ ] 4.4 每個 test dependency 導入並通過 Gradle 後，若 Android app build 受影響，使用 `android-cli` 啟動 app 做 smoke test。

## 5. androidApp Android-only libraries

- [ ] 5.1 確認既有 AndroidX Compose BOM 與 Compose UI dependencies 已使用 catalog alias，執行 Gradle 驗證，成功後用 `android-cli` 啟動 app。
- [ ] 5.2 評估並逐一導入 `androidx-core-ktx`、`androidx-lifecycle-runtime-ktx`、`androidx-lifecycle-runtime-compose`、`androidx-lifecycle-viewmodel-compose`；每個 alias 加入後執行 Gradle 驗證與 `android-cli` app 啟動。
- [ ] 5.3 評估並逐一導入 `androidx-navigation3-runtime`、`androidx-navigation3-ui`、`androidx-paging-runtime`、`androidx-paging-compose`；只有在不需新增產品 UI/feature 的前提下導入，否則記錄跳過原因。
- [ ] 5.4 評估 `coil-compose` 是否只需 Android UI wiring；可安全加入時導入並驗證，否則記錄跳過原因。
- [ ] 5.5 評估 `androidx-work-runtime-ktx`、`androidx-appcompat` 是否符合目前 Android app 架構；不需要者記錄跳過原因，避免無用途依賴。
- [ ] 5.6 確認 Android instrumentation aliases `androidx-test-ext-junit`、`androidx-espresso-core` 只放入 Android test scope；若本 change 不新增 instrumentation test，記錄為暫不導入。

## 6. validation record and final checks

- [ ] 6.1 為每個已導入 alias 記錄 Gradle 命令、結果與 `android-cli` app 啟動結果。
- [ ] 6.2 為每個跳過 alias 記錄分類、跳過原因、已嘗試修復次數與後續建議。
- [ ] 6.3 對單一失敗 alias 最多嘗試三次修復；第三次仍失敗時停止該 alias，更新跳過紀錄並繼續下一個 alias。
- [ ] 6.4 執行最終 Android debug build 或 assemble 驗證。
- [ ] 6.5 執行 shared Android host test；若 iOS source set 有實際變更，執行 iOS simulator 或 metadata 相關 Gradle 驗證。
- [ ] 6.6 使用 `android-cli` 安裝並啟動最終 Android app，確認 app 可開啟到目前 entrypoint。
