## 1. gradle/libs.versions.toml

- [x] 1.1 新增 `skie` version（對應相容 Kotlin 2.4.0 的 SKIE 版本，例如 0.10.13）
- [x] 1.2 新增 SKIE 的 `[plugins]` alias（不新增 library alias，SKIE 無需 runtime dependency）

## 2. shared/app

- [x] 2.1 於 `shared/app/build.gradle.kts` 透過 `alias(libs.plugins.<skie-alias>)` 套用 SKIE plugin
- [x] 2.2 執行既有 iOS framework 建置指令，確認可正常產出且無新增建置錯誤
- [x] 2.3 確認 `androidLibrary` target 與 `androidHostTest` 未受影響（SKIE 只作用於 iOS binary）

## 3. 手動驗證（Swift 端互通行為）

- [x] 3.1 撰寫最小 Swift 驗證程式碼，呼叫 `GetMovieDetailUseCase`（回傳 `Flow<Result<T>>`）並以 `for await` 迭代，確認成功案例可正確取得 `MovieDetailBean`
- [x] 3.2 觸發失敗情境（例如離線或不存在的 `movieId`），確認 Swift 端能正確辨識 `Result.failure`，且不會導致 runtime crash
- [x] 3.3 若 3.1／3.2 驗證發現 SKIE 無法妥善處理 `Flow<Result<T>>`，於 `openspec/backlog.md` 記錄實測結果與後續調整建議，不在本次變更內強行修改 UseCase 簽名

驗證紀錄：`openspec/changes/add-skie/verification/MovieDetailSKIEInteropVerification.swift`
已通過 `swiftc -typecheck`，確認 `GetMovieDetailUseCase.invoke(movieId:)` 可用 `for await`
迭代；但 SKIE 產生的 apinotes 顯示 `Flow<Result<MovieDetailBean>>` 匯出為
`SkieKotlinFlow<id>`，`kotlin.Result<T>` 在 Swift 端被 type erase，後續需另行設計
Swift 友善的結果模型或 wrapper，已記錄於 `openspec/backlog.md`。

## 4. 文件與收尾

- [x] 4.1 確認 `openspec/specs/kmp-dependency-catalog/spec.md`、`openspec/specs/ios-skie-interop/spec.md`（歸檔後）內容與實際套用結果一致
- [x] 4.2 執行 `./gradlew ktlintCheck` 確認新增/修改的 Gradle Kotlin Script 格式通過
