# ios-skie-interop Specification

## Purpose
TBD - created by archiving change add-skie. Update Purpose after archive.
## Requirements
### Requirement: SKIE 只套用於 `shared/app`
專案 MUST 只在 `shared/app`（唯一宣告 iOS `binaries.framework` 的模組）套用 SKIE
Gradle plugin，不得在其他 `shared:*` 子模組個別套用；SKIE 對該 framework
`export()` 的所有依賴模組（`shared:common`、`shared:model`、`shared:data`、
`shared:domain`）生效。

#### Scenario: SKIE 套用於 shared/app
- **WHEN** 檢查 `shared/app/build.gradle.kts`
- **THEN** 套用 SKIE Gradle plugin，版本來自 `gradle/libs.versions.toml` 的 alias

#### Scenario: 其他 shared 模組不重複套用
- **WHEN** 檢查 `shared:common`、`shared:model`、`shared:data`、`shared:domain`、
  `shared:network`、`shared:database`、`shared:datastore` 的 build script
- **THEN** 這些模組 MUST NOT individually 套用 SKIE Gradle plugin

### Requirement: Swift 端以原生 async/await 與 AsyncSequence 消費既有 API
`shared/app` 匯出的 `suspend` function 與 `Flow` 型別 API，SKIE 套用後 SHALL 讓
Swift 端能以原生 `async`/`await` 語法呼叫、以 `AsyncSequence` 語法迭代，不需要
額外撰寫 Objective-C callback wrapper 或手動 annotation。

#### Scenario: suspend function 對應 async/await
- **WHEN** Swift 端呼叫 `shared:app` 匯出、簽名為 `suspend fun` 的 Kotlin API
- **THEN** Swift 端 SHALL 能以 `try await` 語法直接呼叫，不需手動包裝 callback

#### Scenario: Flow 對應 AsyncSequence
- **WHEN** Swift 端消費 `shared:app` 匯出、回傳型別為 `Flow<T>` 的 Kotlin API
- **THEN** Swift 端 SHALL 能以 `for await` 語法迭代該 Flow，且元素型別 `T` 在
  Swift 端保留（不因 Flow 為 interface 而遺失泛型型別資訊）

### Requirement: 已知限制需經手動驗證並留下紀錄
導入 SKIE 前已知的兩項限制（自訂例外無法跨界傳遞至 Swift、`Flow<Result<T>>`
等泛型型別行為未有官方文件明確保證）SHALL 在導入後以手動驗證步驟確認實際行為，
並將驗證結果留下紀錄；若驗證失敗，SHALL 記錄為新的 backlog 項目，不得在未驗證
的狀態下視為可用。

#### Scenario: 驗證 Flow<Result<T>> 的成功與失敗案例
- **WHEN** 實作者以 Swift 呼叫既有回傳 `Flow<Result<T>>` 的 UseCase（例如
  `GetMovieDetailUseCase`），分別觸發成功與失敗（例如離線）情境
- **THEN** Swift 端 SHALL 能正確區分成功與失敗兩種結果，且不因型別轉換問題
  導致 runtime crash 或編譯失敗

#### Scenario: 錯誤路徑不會讓 Swift 端 crash
- **WHEN** 實作者觸發一個會讓 Kotlin 端 Flow 內部發生例外的路徑
- **THEN** 該例外 SHALL 被既有 Repository／UseCase 層轉換為 `Result.failure`
  等資料值後才流出 Flow，不得讓未包裝的自訂例外直接穿越 Flow 到 Swift 端

