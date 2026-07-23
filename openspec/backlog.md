# 開發備忘錄（Backlog）

開發途中發現、留待之後建立新 change 處理的項目，由 /flow-note 維護。

## 評估 flow-discuss 討論產物改為跟隨分支/worktree commit
- 類型: refactor
- 記錄日期: 2026-07-23
- 來源: master
- 前置依賴: 無
- 狀態: 待處理

### 背景

目前 `flow-discuss` 收尾時會直接把討論產物（`openspec/changes/<name>/` 底下的
proposal/design/tasks/`.flow.yaml`）commit + push 到當前所在分支（通常是 master），
之後 `flow-apply` 才從該分支（記錄為 `base_branch`）切出 feature branch 或建立 worktree。

使用者提出疑問：討論文件是否該改成 commit 在 feature branch/worktree 裡，而不是先進 master。

### 目前結論

現況設計是必要的、非疏漏：`flow-apply` 第一步要讀取當前分支上已存在的 `.flow.yaml` 的
`type` 欄位才能決定分支前綴（沒指定 change 名稱時還要用 `openspec list --json` 列出可選項），
這個檔案必須先存在於目前分支才找得到；而 `flow-apply` 的前置檢查又會在工作目錄有未提交變更時
直接停下詢問，因此討論產物必須先在 master commit 乾淨，flow-apply 才能順利往下走。

這個設計的好處是討論產物像 RFC 一樣先落地到 master，任何分支都能看到目前有哪些 change
在規劃中。取捨則是：change 若被放棄，master 會留下一個「文件已存在但功能沒做」的孤兒
commit（無害但會累積）；且文件是直接 push 到 master、沒經過 PR 審查，跟後續實作的 PR
是分開審查的。

若之後要改成「文件也跟著分支/worktree 走」，需要同時修改 `flow-discuss`（討論完不 push，
留在工作目錄）與 `flow-apply`（改成先建分支/worktree，再把未提交的文件一起 commit 上去），
且會跟 flow-apply 現有「有未提交變更就停下詢問」的規則衝突，需要一併調整判斷邏輯。

### 適用範圍

尚未決定是否要改，待評估團隊是否需要「規劃文件也走 PR 審查」或「不想在 master 留下
未實作的孤兒 commit」，之後有需要再回頭處理 `flow-discuss`／`flow-apply` 這兩個 skill。

## 評估 commonMain 通用 AppResult / AppError 取代跨 iOS 匯出的 Kotlin Result
- 類型: refactor
- 記錄日期: 2026-07-23
- 來源: add-skie／ios-splash-rewrite（實作中發現）
- 前置依賴: 無
- 狀態: 待處理

### 背景

`add-skie` 導入 SKIE 後，`GetMovieDetailUseCase.invoke(movieId:)` 已可在 Swift 端以
`for await` 迭代，SKIE 產生的 apinotes 顯示其回傳型別為 `SkieKotlinFlow<id>`。
這代表 `Flow<Result<MovieDetailBean>>` 的 `Flow` 互通已成立，但 `kotlin.Result<T>` 的
泛型成功/失敗語意在 Swift 端被 type erase 成 `Any`，無法以 Swift 型別系統直接辨識
`Result.success(MovieDetailBean)` 與 `Result.failure(Throwable)`。

`ios-splash-rewrite` 除錯確認：SKIE 可以讓 Swift 端正常以 `for await` 消費 `Flow`，
但 `Flow<Result<ConfigurationBean>>` 迭代出的元素在 Swift runtime 會是
`Optional(Success(ConfigurationBean(...)))` 這類 Kotlin `Result` boxed value，而不是
`ConfigurationBean` 本體，因此 Swift 端用 `as? ConfigurationBean` 無法判斷成功。

本次 Splash 先用 `shared/app` iosMain 的 `IosConfigurationLoader` 作為局部 wrapper，
在 Kotlin 端把 `Result<ConfigurationBean>` 拆成 `IosConfigurationLoadState.Success/Failure`
後再匯出給 Swift。這是最小修正，但不應每個 UseCase 都各自手寫一次 bridge。

### 後續調整

1. 評估在 `commonMain` 定義通用 sealed result，例如：
   ```kotlin
   sealed interface AppResult<out T> {
       data class Success<T>(val data: T) : AppResult<T>
       data class Failure(val error: AppError) : AppResult<Nothing>
   }
   ```
2. 不要把所有錯誤都硬轉成 `NetworkException`。`NetworkException` 應維持只代表 network
   request 流程中的錯誤；若要做跨 UseCase 的通用錯誤模型，應另定義更高階的 `AppError` /
   `DomainError`，例如 `Network(NetworkException)`、`Database`、`LocalStorage`、`Unknown`。
3. 若維持既有 UseCase 簽名，評估在 `shared/app` 或 `iosMain` 增加專供 Swift 消費的 wrapper，
   將 `Result<T>` 轉成可被 SKIE 匯出為 Swift enum / class hierarchy 的型別。
4. 長期可評估讓 domain UseCase 從 `Flow<Result<T>>` 漸進改成 `Flow<AppResult<T>>`，
   讓 Android / common / iOS 都消費同一組明確狀態；短期仍可保留局部 iosMain wrapper，
   避免在目前 change 中擴大重構範圍。
5. 補 iOS 端整合測試或 Xcode smoke test，實際覆蓋成功與失敗案例，確認不只 `for await`
   可編譯，也能在 runtime 明確區分成功與失敗。

## iOS 端導入統一 App Log 機制
- 類型: feature
- 記錄日期: 2026-07-23
- 來源: ios-splash-rewrite（實作中發現）
- 前置依賴: 無
- 狀態: 待處理

iOS 端目前除錯主要靠 `print`、Xcode console 與 Ktor network logging。後續需要規劃一套
App log 機制，讓 Swift / shared bridge / network 相關訊息能以一致格式輸出，並能依 Debug /
Release 或環境變數動態控制開關，避免正式版輸出過多敏感或雜訊 log。

## iOS 端多國語言設置
- 類型: feature
- 記錄日期: 2026-07-23
- 來源: ios-splash-rewrite（實作中發現）
- 前置依賴: 無
- 狀態: 待處理

iOS SwiftUI 畫面目前仍直接寫固定文案，例如 Splash 的「載入中」、「準備完成」、「重試」與
錯誤訊息。後續需要補 iOS 端 localization 設置，評估使用 `Localizable.strings` /
String Catalog，並與 shared 層既有語言設定、TMDB API `language` 參數的來源協調，避免
UI 語言與 API 語言狀態不一致。
