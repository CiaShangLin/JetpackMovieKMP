## Context

iOS 端（`iosApp`）收藏切換邏輯目前分散在 5 個 `@Observable` ViewModel：`FavoritesViewModel`、`HomeContentViewModel`、`SearchViewModel`、`HistoryViewModel`、`MovieDetailViewModel`。每個 VM 各自：

1. 用一個 `private var isUpdatingCollection` 旗標 + `guard`/`defer` 防止連點造成 insert/delete 同時執行。
2. 判斷該呼叫 `insertMovieCollect` 還是 `deleteMovieCollect`。多數 VM 透過 `MovieCollectAction(data: MovieCardData)`（定義於 `FavoritesViewModel.swift`）依 `data.movieCardIsCollect` 判斷；`MovieDetailViewModel` 主畫面則因為 `MovieDetailBean → MovieCardResult` 轉換會遺失 `isCollect`，改用另一條 `observeCollectStatus()` 訂閱維護的 `self.isCollect` 判斷。
3. `try await movieRepository.insertMovieCollect(movieResult:)` / `deleteMovieCollect(movieResult:)`，`catch` 只做 `print(...)`。

`MovieRepository` 是 Kotlin 匯出到 Swift 的完整 interface（涵蓋收藏、搜尋、詳情等所有方法），介面過大不易手刻 Fake。目前只有 `SearchViewModel` 為了測試額外定義了窄介面 `MovieCollectionToggling`（只含 insert/delete 兩支方法）與 `MovieRepositoryCollectionAdapter` 轉接具體 `MovieRepository`，其餘 4 個 VM 因為直接持有 `MovieRepository`，收藏切換邏輯完全沒有單元測試。

方法命名也不一致：`toggleMovieCollectStatus`（Favorites/Home/Search/Detail 主畫面）vs `toggleMovieCollect`（History）vs `toggleRecommendCollectStatus`（Detail 推薦清單，語意上是另一個獨立呼叫點，非命名不一致）。

確認過所有呼叫端（`FavoritesView`、`HomeContentView`、`SearchView`、`HistoryView`、`MovieDetailView`）都不會讀取或綁定 `isUpdatingCollection`，這個旗標純粹是 VM 內部防連點用，沒有對外 UI 狀態需求。

## Goals / Non-Goals

**Goals:**
- 把「防連點 guard + insert-or-delete 判斷 + 呼叫 repository + 錯誤處理」這段在 5 處幾乎逐字重複的邏輯，收斂進單一共用類型。
- 讓 `Favorites`／`HomeContent`／`History`／`MovieDetail` 這 4 個目前完全沒有收藏切換測試的 ViewModel，能用跟 `SearchViewModel` 相同的方式（窄 protocol + Fake）補上測試。
- 統一方法命名：`HistoryViewModel.toggleMovieCollect` → `toggleMovieCollectStatus`。
- 移除因此變得多餘的 `MovieCollectAction` enum。

**Non-Goals:**
- 不新增 Kotlin `ToggleFavoriteUseCase` 或任何 `shared/domain`、`shared/data` 變更；Android 端 `feature/collect/CollectViewModel` 不受影響、不強制共用此次抽出的 Swift 元件。
- 不修正 `MovieDetailBean → MovieCardResult` 轉換遺失 `isCollect` 的既有問題；`MovieDetailViewModel.observeCollectStatus()` 這條獨立訂閱機制維持原樣，本次只收斂「拿到 isCollect 之後怎麼呼叫 repository」這段。
- 不對外新增 loading/error UI 狀態；錯誤處理維持現況的 `print(...)`，不在本次一併升級成使用者可見的錯誤提示（若日後需要，屬另一個獨立變更）。

## Decisions

### 決策 1：新增 `MovieCollectToggler` 類別，取代各 VM 自行實作的 guard + 判斷邏輯

**選擇**：在新檔案 `iosApp/iosApp/Common/MovieCollectToggler.swift` 新增：
- 沿用（搬移）既有的 `MovieCollectionToggling` protocol（只含 `insertMovieCollect(movieResult:)` / `deleteMovieCollect(movieResult:)`）與 `MovieRepositoryCollectionAdapter`（轉接具體 `MovieRepository`）。
- `final class MovieCollectToggler`，`@MainActor`、持有 `private let repository: MovieCollectionToggling` 與 `private var isUpdatingCollection = false`，對外單一方法：
  ```swift
  func toggle(currentIsCollect: Bool, movie: MovieCardResult) async {
      guard !isUpdatingCollection else { return }
      isUpdatingCollection = true
      defer { isUpdatingCollection = false }
      do {
          if currentIsCollect {
              try await repository.deleteMovieCollect(movieResult: movie)
          } else {
              try await repository.insertMovieCollect(movieResult: movie)
          }
      } catch {
          print("切換收藏失敗：\(error.localizedDescription)")
      }
  }
  ```
- 各 VM 建構子改為接收 `MovieCollectToggler`（預設值 `MovieCollectToggler(repository: MovieRepositoryCollectionAdapter(repository: KoinHelper.shared.getMovieRepository()))`，維持現有各 VM 「建構子注入 + 預設值走 KoinHelper」的慣例）。

**為什麼**：`MovieRepository`（完整 Kotlin interface）與「toggle 這個動作要做的事」是兩個不同層次的關注點；把後者收斂成一個獨立、依賴窄 protocol 的類型，同時解決重複程式碼與測試缺口兩個問題，且不影響 `shared` 層。

**考慮過的替代方案**：
- **在 `shared/domain` 新增 `ToggleFavoriteUseCase`**：可讓 Android/iOS 共用同一份 insert-or-delete 判斷邏輯，但範圍會擴大到需同時調整 Android `CollectViewModel`、`shared/domain`、`shared/app` DI 組裝；本次使用者已確認只處理 iOS 端重複問題，故不採用（可留待未來若 Android 也出現同類重複時再議）。
- **用 protocol extension 或 free function 取代 class**：`MovieCollectToggler` 需要保存 `isUpdatingCollection` 這個跨 await 呼叫的 mutable 狀態，protocol extension 沒有儲存屬性、free function 沒有地方存放狀態，因此改用 class（且需 `@MainActor` 確保狀態存取安全）。

### 決策 2：呼叫端一律先算出 `(currentIsCollect: Bool, movie: MovieCardResult)` 再委派給 toggler，不在 toggler 內部處理型別轉換

**選擇**：`MovieCollectToggler.toggle` 只吃 `(currentIsCollect: Bool, movie: MovieCardResult)`。呼叫端各自負責把自己拿到的資料轉成這兩個值：
- 吃 `MovieCardData` 的 VM：`toggler.toggle(currentIsCollect: data.movieCardIsCollect, movie: data.asMovieCardResult())`
- `MovieDetailViewModel` 主畫面（吃 `MovieCardResult`，`isCollect` 來自 `observeCollectStatus()`）：`toggler.toggle(currentIsCollect: isCollect, movie: data)`

**為什麼**：三種呼叫端（`MovieCardData` 自帶 isCollect／`MovieCardResult` 直傳配合外部 isCollect state／未來可能出現的其他型別）取得 `isCollect` 的方式本質不同，硬塞進 toggler 內部處理只會把型別判斷邏輯換個地方繼續分散；讓呼叫端做這一行轉換，toggler 才能保持型別單純、依賴淺。

**考慮過的替代方案**：
- **toggler 內部 overload 出多個 `toggle(data: MovieCardData)` / `toggle(data: MovieCardResult)`**：仍然需要在 toggler 內部重建一份「怎麼從各型別算出 isCollect」的邏輯，且 `MovieDetailViewModel` 的 isCollect 來源本來就不在傳入的資料裡，無法用單純的型別 overload 表達，故不採用。

### 決策 3：移除 `MovieCollectAction` enum，改寫對應測試到 `MovieCollectTogglerTests`

**選擇**：`MovieCollectAction` 原本的職責（依 `MovieCardData.movieCardIsCollect` 判斷 insert 或 delete）已完全被 `MovieCollectToggler.toggle` 內的 `if currentIsCollect` 取代，直接刪除該 enum；`iosApp/iosAppTests/Favorites/MovieCollectActionTests.swift` 的測試案例改寫進新的 `iosApp/iosAppTests/Common/MovieCollectTogglerTests.swift`（測試 toggler 本身：insert 分支、delete 分支、連點時第二次呼叫被 guard 擋下、repository 拋錯時不 crash）。

**為什麼**：保留一個不再被使用、只做「型別轉換 + 判斷」的 enum 會製造混淆（未來讀者需要理解為什麼 `MovieCollectAction` 和 `MovieCollectToggler` 都在做類似的事）；既然邏輯已經完整搬移，直接移除比保留一個死程式碼路徑更清楚。

### 決策 4：統一命名 `toggleMovieCollectStatus`，`HistoryView.swift` 同步更新呼叫端

**選擇**：`HistoryViewModel.toggleMovieCollect(data:)` 改名為 `toggleMovieCollectStatus(data:)`，與 Favorites/Home/Search/Detail 一致；`HistoryView.swift:99` 的呼叫點同步改名。`MovieDetailViewModel.toggleRecommendCollectStatus` 維持原名不變（它是詳情頁推薦清單這個獨立呼叫點的名稱，語意上不是同一個方法，不屬於命名不一致問題）。

**為什麼**：5 個 VM 中有 4 個已經用 `toggleMovieCollectStatus`，只有 History 用不同名稱，統一成多數命名成本最低、對其他呼叫端影響最小。

## Risks / Trade-offs

- **[風險] `MovieCollectToggler` 的 `isUpdatingCollection` 狀態在多個 VM 之間若不小心共用同一個實例，會讓不相關的收藏操作互相阻擋（例如 Favorites 頁面正在切換時，Home 頁面的切換也會被擋住）** → 緩解：每個 ViewModel 各自持有並注入**獨立的** `MovieCollectToggler` 實例（不做成 singleton／不透過 Koin 注入單一共用實例），維持現況「每個 VM 各自防連點」的行為邊界不變。
- **[風險] 移除 `MovieCollectAction` enum 若有其他測試檔案或程式碼隱性依賴它（目前只找到 `MovieCollectActionTests.swift` 引用），可能造成編譯失敗** → 緩解：實作階段先搬移/改寫測試，再刪除 enum，並確保 `iosApp` 整個 target 編譯通過後才視為完成。
- **[風險] `HistoryViewModel` 方法改名屬於 breaking change（雖僅限 iOS 內部呼叫），若遺漏更新呼叫端會編譯失敗，但不會有執行期靜默錯誤** → 緩解：Swift 編譯器會直接在 `HistoryView.swift` 報錯，風險可在建置階段被攔截，非執行期風險。
- **[Trade-off] `MovieDetailViewModel` 仍維持雙軌（`observeCollectStatus()` 額外訂閱 + toggler 委派），沒有徹底統一收藏狀態的 source of truth** → 這是本次刻意縮小範圍的決定（見 Non-Goals），根因是 `MovieDetailBean` 缺少 `isCollect` 欄位，需要改動 `shared/model`／`shared/data` 才能徹底解決，留待未來獨立 change 處理。

## Migration Plan

1. 新增 `MovieCollectToggler.swift`（含搬移的 protocol/adapter），先讓新舊程式碼並存、不刪除任何既有檔案，確保專案可編譯。
2. 依序改寫 5 個 ViewModel 改用 `MovieCollectToggler`，每改完一個就跑一次該 VM 既有測試（若有）與手動確認呼叫端編譯通過。
3. `HistoryViewModel` 改名同步更新 `HistoryView.swift`。
4. 補齊 `Favorites`／`HomeContent`／`History`／`MovieDetail` 的收藏切換單元測試。
5. 確認全部呼叫端改用新元件後，刪除 `MovieCollectAction` enum 與舊的 `MovieCollectActionTests.swift`（內容已搬進 `MovieCollectTogglerTests.swift`）。
6. 全專案跑 `iosFormat`、`iosLint`、iOS simulator test，確認無殘留引用與格式問題。

無需 feature flag 或分階段上線：純內部程式碼結構調整，行為對使用者完全不可見，不需回滾策略（有問題直接修正或 revert commit 即可）。

## Open Questions

（無未解決問題；範圍邊界已於討論階段與使用者確認：只重構 iOS Swift 端，不動 shared 層與 `MovieDetailBean.isCollect` 根因。）
