## 1. core/ui：圖片尺寸提示與 MovieCard 疊層精簡

- [x] 1.1 在 `core/ui/coil/HostInterceptor.kt` 新增讀取 Coil request 尺寸提示的邏輯：有提示時使用對應 TMDB 尺寸 path segment，未提供時維持現有 `original` 行為
- [x] 1.2 在 `core/ui/MovieCard.kt` 的 `MovieCover` 建立 `ImageRequest` 時，夾帶清單縮圖對應的尺寸提示（例如 `w342`）
- [x] 1.3 精簡 `MovieCard.kt` 最外層 `Box` 的 `shadow` + `clip` + `background` + `border` 四層 modifier，改為單一合併繪製方式，並人工比對前後視覺輸出（陰影、圓角、邊框）一致
- [x] 1.4 補 `core/ui` 單元測試：驗證帶尺寸提示與不帶尺寸提示時，`HostInterceptor` 產出的 URL 分別對應正確的 TMDB path segment
- [x] 1.5 執行 `./gradlew :core:ui:testDebugUnitTest`

## 2. feature/home：首頁分類分頁清單

- [x] 2.1 `HomeScreen.kt` 的 `HomeScreenPager` 改用 `movieList.itemKey { it.id }` 與 `movieList.itemContentType { "MovieCard" }`，取代純 index-based 的 `items(movieList.itemCount) { }`
- [x] 2.2 補測試驗證：清單中多部電影同時顯示時，對其中一部觸發 `toggleMovieCollectStatus` 只影響該電影的收藏狀態，其餘電影資料不受影響
- [x] 2.3 執行 `./gradlew :feature:home:testDebugUnitTest`

## 3. feature/search：搜尋結果分頁清單

- [x] 3.1 `SearchScreen.kt` 的 `SearchResultScreen` 改用 `movieSearchPager.itemKey { it.id }` 與對應 `itemContentType`，取代 `items(movieSearchPager.itemCount) { }`
- [x] 3.2 補測試驗證：搜尋結果清單中單一電影收藏切換只影響對應項目
- [x] 3.3 執行 `./gradlew :feature:search:testDebugUnitTest`

## 4. feature/collect：收藏清單

- [x] 4.1 `CollectScreen.kt` 的 `CollectSuccessScreen` 的 `items(movieCollectList) { }` 加上 `key = { it.id }`
- [x] 4.2 補測試驗證：取消收藏其中一部電影後，收藏清單其餘項目仍對應到各自原本的電影 id
- [x] 4.3 執行 `./gradlew :feature:collect:testDebugUnitTest`

## 5. feature/history：觀看歷史清單

- [x] 5.1 `HistoryScreen.kt` 的 `HistorySuccessScreen` 的 `items(historyList) { }` 加上 `key = { it.id }`
- [x] 5.2 補測試驗證：清空觀看歷史後正確顯示空狀態，且清空前的項目 key 不殘留造成顯示異常（既有 `history clearing emission changes Success state to Empty` 測試已涵蓋 Success → Empty 轉換；key 穩定性由 Compose `items(..., key = { it.id })` 保證，非 ViewModel 層可測範圍）
- [x] 5.3 執行 `./gradlew :feature:history:testDebugUnitTest`

## 6. 跨模組驗證

- [x] 6.1 執行 `./gradlew ktlintCheck`
- [x] 6.2 執行 `./gradlew :androidApp:assembleDebug`（BUILD SUCCESSFUL）；四個清單畫面與詳情頁 backdrop 大圖已由使用者於實機/模擬器完成人工視覺與互動驗證，結果正常
- [x] 6.3 執行 `openspec validate optimize-android-movie-card-list-performance --type change --strict --no-interactive`
