## 1. shared/data：`MovieGenrePagingSource` 跨頁去重

- [ ] 1.1 在 `MovieGenrePagingSource` 新增 instance 欄位（`MutableSet<Int>`）追蹤已載入的電影 `id`
- [ ] 1.2 `load()` 成功取得回應後，過濾掉集合中已存在 `id` 的項目，再把剩餘項目的 `id` 併入集合，最後組成 `LoadResult.Page`
- [ ] 1.3 確認 `prevKey`／`nextKey`／`totalPages` 計算邏輯不變，僅依伺服器回應中繼資料，不受過濾後筆數影響

## 2. shared/data：`MovieSearchPagingSource` 套用相同去重邏輯

- [ ] 2.1 在 `MovieSearchPagingSource` 套用與 `MovieGenrePagingSource` 相同的跨頁去重邏輯

## 3. shared/data：測試

- [ ] 3.1 `MovieGenrePagingSourceTest` 新增案例：連續兩次 `load()`，第二次回應包含第一次已出現的 `id`，驗證 `LoadResult.Page.data` 過濾掉重複項目
- [ ] 3.2 `MovieGenrePagingSourceTest` 新增案例：去重後頁面筆數變少甚至為 0 時，`nextKey` 仍依 `totalPages` 正常回傳（不因過濾筆數被誤判為最後一頁）
- [ ] 3.3 `MovieSearchPagingSourceTest` 新增對應的跨頁重複 `id` 過濾案例
- [ ] 3.4 新增或確認既有測試涵蓋：新建立的 `PagingSource` 實例（模擬 refresh 後重新建立）已載入 `id` 追蹤為空集合，先前 session 出現過的 `id` 不會被視為重複
- [ ] 3.5 執行 `./gradlew :shared:data:testAndroidHostTest --tests "com.shang.jetpackmoviekmp.data.paging.MovieGenrePagingSourceTest"` 與對應的 `MovieSearchPagingSourceTest`，確認全數通過
- [ ] 3.6 執行 `./gradlew :shared:data:koverVerify`，確認新增程式碼路徑符合 80% 覆蓋率下限

## 4. 最終驗證

- [ ] 4.1 執行 `./gradlew ktlintCheck`
- [ ] 4.2 執行 `./gradlew :shared:data:testAndroidHostTest`（完整模組測試，非僅單一測試類別）
- [ ] 4.3 執行 `openspec validate fix-home-paging-duplicate-key --type change --strict --no-interactive`
