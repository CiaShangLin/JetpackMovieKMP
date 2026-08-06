## 1. Git 修正（優先處理，避免簽章檔進入 commit 歷史）

- [ ] 1.1 執行 `git restore --staged JetpackMovieCompose.jsk`，移除 staging 但保留本機實體檔案
- [ ] 1.2 於根目錄 `.gitignore` 新增排除規則：`*.jks`、`*.jsk`、`keystore.properties`
- [ ] 1.3 執行 `git status` 確認 `JetpackMovieCompose.jsk` 與 `keystore.properties` 皆未被追蹤

## 2. Keystore 設定檔（根目錄）

- [ ] 2.1 新增 `keystore.properties.example`（進版控），欄位為 `storeFile`、`storePassword`、`keyAlias`、`keyPassword`，值使用明顯佔位字串（如 `CHANGE_ME`）
- [ ] 2.2 於本機建立 `keystore.properties`（不進版控），依 `keystore.properties.example` 欄位填入實際值：`storeFile=JetpackMovieCompose.jsk`、`storePassword=jetpackmoviekmp`、`keyAlias=jetpackmoviekmp`、`keyPassword=jetpackmoviekmp`

## 3. androidApp：簽章設定

- [ ] 3.1 於 `androidApp/build.gradle.kts` 新增 `java.util.Properties` 讀取 rootProject `keystore.properties` 的邏輯（比照 `shared/network/build.gradle.kts` 讀取 `key.properties` 的寫法，檔案不存在時各欄位給空字串預設值）
- [ ] 3.2 於 `android {}` block 新增 `signingConfigs.create("release")`，設定 `storeFile`、`storePassword`、`keyAlias`、`keyPassword` 對應讀取到的屬性值
- [ ] 3.3 將 `buildTypes.getByName("release")` 的 `signingConfig` 指向 `signingConfigs.getByName("release")`

## 4. androidApp：R8 / ProGuard 混淆設定

- [ ] 4.1 新增 `androidApp/proguard-rules.pro`，先建立基礎骨架（含註解說明各段用途）
- [ ] 4.2 於 `buildTypes.getByName("release")` 設定 `isMinifyEnabled = true`、`isShrinkResources = true`，並設定 `proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")`
- [ ] 4.3 確認 `benchmark` build type（`initWith(buildTypes.getByName("release"))`）在調整後仍維持 `signingConfig = signingConfigs.getByName("debug")` 且行為不受影響

## 5. 建置與功能驗證

- [ ] 5.1 執行 `./gradlew :androidApp:assembleRelease`（Windows: `.\gradlew.bat :androidApp:assembleRelease`），確認建置成功且產出已簽章 APK
- [ ] 5.2 安裝混淆後的 release APK 至實體裝置或模擬器，手動操作首頁列表、詳情頁、搜尋、收藏、歷史、設定，確認皆正常運作、無因缺少 keep 規則導致的閃退或注入失敗
- [ ] 5.3 若步驟 5.2 發現因混淆導致的例外，回頭補充 `androidApp/proguard-rules.pro` 對應 keep 規則，並重新執行 5.1、5.2 直到功能正常

## 6. Lint 與最終驗證

- [ ] 6.1 執行 `./gradlew ktlintCheck` 確認通過
- [ ] 6.2 執行 `openspec validate android-release-signing --type change --strict --no-interactive` 確認 change 文件通過驗證
