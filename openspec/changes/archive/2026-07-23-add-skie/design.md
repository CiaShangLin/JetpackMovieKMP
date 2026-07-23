## Context

`shared/app` 是唯一宣告 iOS `binaries.framework`（`baseName = "Shared"`）的模組，
`export()` 了 `shared:common`、`shared:model`、`shared:data`、`shared:domain`。
`iosApp` 已透過 `ios-koin-bridge` 規格定義的 `KoinHelper` 具名橋接物件解析
`UserDataRepository`、`MovieRepository`、5 個 UseCase 等依賴，走的是 Kotlin/Native
預設的 Objective-C export 路徑。domain 層 UseCase 目前的公開簽名多半回傳
`Flow<Result<T>>`（`GetMovieDetailUseCase`、`GetMovieRecommendUseCase`）或
`Flow<PagingData<T>>`（`GetHomeMovieListUseCase`），這些型別在純 Objective-C
橋接下，Swift 端只能用較笨重的 `Kotlinx_coroutines_coreFlow` + callback 操作。

專案 Kotlin 版本為 2.4.0（見 `gradle/libs.versions.toml`）。查證官方 release note，
Kotlin 2.4.0 同時原生新增了 **Swift Export**（Alpha 階段），也能把 `Flow` 匯出為
Swift `AsyncSequence`；但 Swift Export 與現有的 Objective-C export **互斥**，
且需要改變框架產出方式（原生 Swift module 而非 `.framework` + Obj-C header），
與目前 `shared/app` 已上線的 `iosTarget.binaries.framework` 架構、`KoinHelper`
橋接慣例衝突面過大，不適合在本次變更中一併切換。

## Goals / Non-Goals

**Goals:**
- 在 `shared/app` 套用 SKIE Gradle plugin，讓既有 `suspend` function 與 `Flow`
  匯出時自動產生 Swift `async/await`、`AsyncSequence` 橋接，不需手寫 wrapper。
- 依專案既有 version catalog 慣例，將 SKIE 版本集中管理在 `gradle/libs.versions.toml`。
- 驗證 SKIE 對本專案既有 UseCase 簽名（尤其 `Flow<Result<T>>`）的實際匯出行為，
  確認可用或找出限制。

**Non-Goals:**
- 不在本次變更中導入 Kotlin 原生 Swift Export（Alpha、與 Obj-C export 互斥）。
- 不在本次變更中調整任何 UseCase / Repository 的 public 簽名（例如把
  `Flow<Result<T>>` 改成 sealed class）；若驗證後發現有必要，留待後續 change。
- 不涉及 Android 端（`androidApp`、`core/*`）；SKIE 只作用於 iOS framework 輸出。

## Decisions

### Decision 1：SKIE 只套用在 `shared/app`，不逐一套用到每個 `shared/*` 模組
依 SKIE 官方文件，plugin 只需套用在「宣告 Xcode framework 的模組」，套用後會對
該 framework `export()` 的所有依賴模組程式碼生效（不限於套用 plugin 的模組本身）。
`shared/app` 是本專案唯一符合條件的模組，套用一次即可涵蓋 `common`／`model`／
`data`／`domain` 匯出的所有 API，不需要、也不應該在 `shared:domain` 等模組
個別套用。

### Decision 2：版本管理沿用 `gradle/libs.versions.toml`，不採 SKIE 官方 quickstart
文件常見的直接在 module `build.gradle.kts` 寫死版本號寫法
SKIE 官方安裝文件範例常直接在套用 plugin 處寫版本號字串。本專案既有慣例（Ktor、
Room、Coroutines 等）一律走 version catalog alias，新增 `skie` version 與對應
plugin alias 維持一致性，避免版本號分散在多處。

### Decision 3：本次維持 Objective-C export + SKIE，不切換 Swift Export
考量：
1. Swift Export 目前仍是 Alpha，官方文件明確標註生產環境需謹慎評估。
2. 兩者互斥，切換需重寫 `shared/app` 的 framework 匯出設定與 `iosApp` 消費方式，
   影響範圍遠超過「導入一個編譯器 plugin」的原始需求。
3. SKIE 是成熟度較高（0.10.x 系列已跟隨 Kotlin 各版本穩定跟進，含 2.4.0）的
   第三方方案，可用最小改動達成本次目的。

Swift Export 轉為 Stable 後是否遷移，留給未來獨立 change 評估，不在本次 backlog
範圍內處理。

### Decision 4：不預先調整 `Flow<Result<T>>` 型態，先做最小可行導入 + 手動驗證
SKIE 官方文件對 `Flow<T>` 泛型參數為 `kotlin.Result` 時的行為沒有明確文件化。
與其在導入前臆測並重構既有 UseCase 簽名（違反「修改範圍聚焦在使用者要求的行為」
原則），本次先只套用 plugin、建置出 framework，再以最小 Swift 驗證程式碼實際
呼叫一個既有回傳 `Flow<Result<T>>` 的 UseCase（見 tasks.md 的驗證步驟），依實測
結果決定是否需要後續 change 調整簽名。

## Risks / Trade-offs

- **[Risk] `Flow<Result<T>>` 在 SKIE 轉換下的 Swift 端行為未有官方文件明確保證**
  → Mitigation：導入後以 tasks.md 規劃的手動驗證步驟實際呼叫
  `GetMovieDetailUseCase`／`GetMovieRecommendUseCase`，確認 Swift 端可正確取得
  `Result.success`／`Result.failure` 兩種情境的值；若行為不可用，記錄為新的
  backlog 項目而非本次強行修改簽名。
- **[Risk] SKIE 文件指出「Flow 內部拋出的自訂例外無法正確傳遞到 Swift，會導致
  runtime crash」** → Mitigation：檢查本專案 UseCase／Repository 目前的錯誤處理
  慣例（`MovieRepositoryImpl` 已把例外轉換為 `Result.failure` 而非讓例外穿越
  Flow），此慣例已與 SKIE 限制相容；驗證步驟中額外確認一次錯誤路徑（例如離線時
  呼叫 `GetMovieDetailUseCase`）不會導致 Swift 端 crash。
- **[Risk] iOS framework 建置時間增加**（SKIE 需額外產生並編譯 Swift 橋接層）
  → Mitigation：可接受的一次性成本，不影響 Android 建置與 CI 中 `androidHostTest`
  ／`koverVerify` 的執行時間。
- **[Risk] 對 Objective-C export 的依附，與未來 Kotlin Swift Export 路線存在
  技術債** → Mitigation：Decision 3 已明確記錄此取捨，待 Swift Export 轉 Stable
  後再排入獨立 change 評估遷移，不在本次范圍內強求兩全。

## Migration Plan

1. `gradle/libs.versions.toml` 新增 `skie` version 與 plugin alias。
2. `shared/app/build.gradle.kts` 套用該 plugin（無需額外 `dependencies` 設定，
   SKIE 是編譯期 plugin，不需要 runtime library）。
3. 執行既有 iOS framework 建置指令，確認可正常產出（不引入新的建置錯誤）。
4. 撰寫最小 Swift 驗證程式碼，呼叫至少一個 `Flow<Result<T>>` UseCase 與一個
   `suspend` 型 API（若有），確認 async/await、AsyncSequence 語法可用。
5. 若驗證發現問題，記錄到 `openspec/backlog.md`，不在本次 tasks 內展開修復。

**Rollback**：本次變更未修改任何既有 public 簽名，只新增 plugin 套用與 version
catalog 條目；回退只需移除 `shared/app/build.gradle.kts` 的 plugin 套用與
`libs.versions.toml` 的 alias，即可完全還原，不影響其他模組。

## Open Questions

- `Flow<Result<T>>` 在 SKIE 轉換後的實際 Swift 端型態與錯誤處理方式，需在
  tasks.md 的手動驗證步驟中得到答案，目前僅能依官方文件推論。

已確認事項（不再是 open question）：SKIE 已於 Touchlab 官方公告全面開源，採
Apache-2.0 授權，無商業使用限制，可直接引入不需額外授權評估。
