## Context

`gradle/libs.versions.toml` 已集中定義 KMP、Android、測試與 plugin aliases，但實際 build scripts 仍需按用途逐步引用。專案目前以 `shared` 承載 KMP 共用邏輯，`androidApp` 承載 Android app entrypoint；iOS 可在需要平台 engine 或 KMP target wiring 時補最小配置，但本 change 不主動開發 iOS UI。

這次變更不調整 MVVM / MVI / Repository / Use Case 架構本身，只為後續架構層引入必要依賴。若某個 library 需要建立最小 usage 才能確認可編譯，實作應放在對應 module/source set 的最小範圍內，避免先行新增產品行為。

## Goals / Non-Goals

**Goals:**

- 依 KMP 支援程度排序引入 catalog 內 library。
- 每次引入後用 Gradle 驗證編譯，再用 `android-cli` 啟動 Android app 做 smoke test。
- 將 Android-only dependency 限制在 `androidApp` 或 Android source set。
- 將每個失敗 dependency 的嘗試次數、修復動作與跳過原因留下紀錄。

**Non-Goals:**

- 不新增電影列表、網路 API、資料庫 DAO、分頁或快取等產品功能。
- 不主動建立 iOS app UI 或 iOS smoke test。
- 不導入 `libs.versions.toml` 以外的新 dependency，除非某個已列 alias 缺少必要 runtime 配套且必須補齊。
- 不重新引入 Compose Multiplatform 或 JetBrains Compose artifacts。

## Decisions

1. 依 dependency 支援範圍分批，不依 TOML 文字順序硬套。
   - 選擇：先處理 KMP/common 可用依賴，再處理需要平台分流的 KMP 依賴，最後處理 Android-only。
   - 替代方案：完全照 TOML 順序引入。缺點是 Android-only 依賴可能先污染共用 source set，也較難維持 KMP 邊界。

2. 每次只引入一個 library；若 library 需成組才可解析或使用，才以最小不可拆分群組處理。
   - 選擇：例如 Ktor `client-core` 可先進 commonMain，平台 engine 則分別進 android/iOS source set；Compose BOM 與 AndroidX Compose artifacts 視為 Android UI 的一組。
   - 替代方案：一次引入整個生態系。缺點是失敗時難定位是哪個 artifact 或 source set 配置造成。

3. 驗證採「Gradle 先、Android smoke test 後」。
   - 選擇：每輪先執行對應 Gradle 任務，成功後才透過 `android-cli` 安裝/啟動 app。
   - 替代方案：只跑完整 `check`。缺點是時間成本高，且無法滿足每個 dependency 引入後確認 app 可開啟的需求。

4. 單一 dependency 最多修復 3 次。
   - 選擇：每次失敗先判斷是版本解析、source set 邊界、plugin wiring 或 API 使用錯誤；第 3 次仍失敗就記錄跳過。
   - 替代方案：卡在同一 dependency 直到成功。缺點是不符合使用者要求，也會阻塞其他低風險依賴。

5. Room 相關只做 dependency wiring，不做 schema 或 DAO 實作。
   - 選擇：導入 Room plugin、KSP plugin、`room-runtime`、`room-paging`、`sqlite-bundled` 與 Room compiler target wiring，並建立受版控 schema directory，但不建立資料表。
   - 替代方案：等 database layer change 才導入 Room。缺點是本次 catalog adoption 無法驗證 Room KMP plugin/compiler wiring。
   - 替代方案：同時建立 database layer。缺點是超出 dependency adoption baseline。

6. Paging3 分成 KMP common 與 Android UI 兩層導入。
   - 選擇：`androidx-paging-common` 放在 `shared.commonMain`，`androidx-paging-runtime` 與 `androidx-paging-compose` 放在 `androidApp`。
   - 替代方案：只在 Android app 加 Paging。缺點是忽略 Paging3 的 KMP/common 支援，後續 shared repository/use case 仍缺基線依賴。

## Risks / Trade-offs

- [Risk] 某些 alias 雖支援 KMP，但目前專案 source set 或 plugin 尚未準備好。→ Mitigation: 先調整最小 Gradle wiring，若 3 次仍失敗就記錄並跳過。
- [Risk] Android smoke test 需要可用裝置或模擬器。→ Mitigation: 使用 `android-cli` 檢查裝置狀態；若無裝置，將該輪記為環境阻塞而非 dependency 失敗。
- [Risk] 成組引入可能違反「每次一個」的可定位性。→ Mitigation: 僅允許不可拆分的 runtime/plugin/compiler/BOM pairing，並在 tasks 紀錄群組原因。
- [Risk] Room 未建立 schema 時導入 plugin 可能需要額外設定。→ Mitigation: 本 change 不新增 database schema；只加入 schema directory、plugin、compiler 與 runtime wiring，不建立 migration。
- [Risk] Windows host 無法執行 iOS simulator tests。→ Mitigation: 保留 Room KSP iOS target wiring，並以 Android assemble/shared host test 驗證目前可執行路徑；iOS simulator 留待 macOS 環境驗證。
- [Risk] `openspec` telemetry 在受限網路下可能輸出 PostHog 錯誤。→ Mitigation: 僅以 CLI exit code 與 artifact 狀態判定 OpenSpec 是否成功。
