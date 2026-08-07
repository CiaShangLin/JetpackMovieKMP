# 開發備忘錄（Backlog）

開發途中發現、留待之後建立新 change 處理的項目，由 /flow-note 維護。

## 整理 iOS 資料夾分類

- 類型: refactor
- 記錄日期: 2026-08-02
- 來源: master
- 前置依賴: 無
- 狀態: 待處理

規劃 iOS 端資料夾分類。


## 討論簡化 iOS 與 Android 端 AppResult 消費方式，看能否減少重複樣板

- 類型: refactor
- 記錄日期: 2026-08-03
- 來源: add-ios-movie-detail（實作中發現）
- 前置依賴: 無
- 狀態: 待處理

目前 iOS 端每個消費 `AppResult<T>` 的地方（`HomeViewModel.loadHome()`、
`MovieDetailViewModel.fetchMovieDetail()` 等）都要重複同一套樣板：
`switch onEnum(of: result) { .success: success.data as? T cast; .failure: switch
onEnum(of: failure.error) { .network / .unknown } }`。Android 端則是
`when (result) { is AppResult.Success -> ...; is AppResult.Failure -> ... }`
搭配各自的 `result.error` 處理，也有類似的重複判斷邏輯散落在多個 ViewModel。

討論中也連帶提到 `DetailSectionState<T>`（Android `feature/detail` 既有的區塊級
Loading/Success/Error 狀態）要不要放進 `shared/common` 讓 iOS 共用泛型型別；當下
結論是先不共用（`shared/common` 定位為跨層共用型別而非畫面呈現狀態，且透過 Kotlin
generic 匯出到 iOS 需要 `onEnum(of:)` 橋接，比原生 Swift generic enum 更麻煩），
iOS 端另外寫原生 Swift enum 因應。

之後有餘裕時，評估是否能在 iOS 端封裝一個共用 helper（例如把 `onEnum(of:)` 兩層
switch 包成一個回傳 `Result<T, AppError>`或類似結構的 extension function），
以及 Android 端是否也有等效的重複判斷可以抽共用 mapper，減少兩平台個別消費
`AppResult` 時的重複樣板程式碼。

## 設定 Android Release 的 GitHub Actions CI/CD 流程

- 類型: feature
- 記錄日期: 2026-08-06
- 來源: master
- 前置依賴: 無
- 狀態: 待處理

需要建立/調整 GitHub Actions workflow，串接 androidApp 現有的 release 簽章與 R8
混淆設定（參考 ce2673e），自動化 Android release 的建置、簽章與發布流程。

## 修正切換底部 Tab 時首頁重新觸發 API 的問題

- 類型: bug-fix
- 記錄日期: 2026-08-07
- 來源: add-release-ci-workflow（實作中發現）
- 前置依賴: 無
- 狀態: 待處理

目前 Android 在切換底部 Tab 時，只要切到其他 Tab 後再切回首頁（或切換到其他
Tab 的當下），首頁就會重新觸發一次 API 請求。需要討論是不是 Navigation3 的
scene/backstack 生命週期造成畫面重組導致重新請求，或回頭檢查之前對 Nav3 導覽
做過的調整是否有影響到這裡，找出根因後再決定怎麼調整（例如用 rememberSaveable、
ViewModel scope 綁定導覽層級，或調整 LaunchedEffect 的觸發條件）。