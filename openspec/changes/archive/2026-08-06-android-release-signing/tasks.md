## 1. Git 修正（優先處理，避免簽章檔進入 commit 歷史）

- [x] 1.1 執行 `git restore --staged JetpackMovieCompose.jsk`，移除 staging 但保留本機實體檔案（實際發現該檔案已被 commit 進 master history，非僅 staged；改用 `git rm --cached` 移除追蹤，經與使用者確認此為個人小專案，不需處理 history）
- [x] 1.2 於根目錄 `.gitignore` 新增排除規則：`*.jks`、`*.jsk`、`keystore.properties`
- [x] 1.3 執行 `git status` 確認 `JetpackMovieCompose.jsk` 與 `keystore.properties` 皆未被追蹤

## 2. Keystore 設定檔（根目錄）

- [x] 2.1 新增 `keystore.properties.example`（進版控），欄位為 `storeFile`、`storePassword`、`keyAlias`、`keyPassword`，值使用明顯佔位字串（如 `CHANGE_ME`）
- [x] 2.2 於本機建立 `keystore.properties`（不進版控）：舊 `JetpackMovieCompose.jsk` 密碼已遺失且無法驗證，經與使用者確認後改用 keytool 重新產生 `JetpackMovieKMP.jks`，`storeFile=JetpackMovieKMP.jks`、`storePassword=jetpackmoviekmp`、`keyAlias=jetpackmoviekmp`、`keyPassword=jetpackmoviekmp`

## 3. androidApp：簽章設定

- [x] 3.1 於 `androidApp/build.gradle.kts` 新增 `java.util.Properties` 讀取 rootProject `keystore.properties` 的邏輯（比照 `shared/network/build.gradle.kts` 讀取 `key.properties` 的寫法，檔案不存在時各欄位給空字串預設值）
- [x] 3.2 於 `android {}` block 新增 `signingConfigs.create("release")`，設定 `storeFile`、`storePassword`、`keyAlias`、`keyPassword` 對應讀取到的屬性值
- [x] 3.3 將 `buildTypes.getByName("release")` 的 `signingConfig` 指向 `signingConfigs.getByName("release")`

## 4. androidApp：R8 / ProGuard 混淆設定

- [x] 4.1 新增 `androidApp/proguard-rules.pro`，先建立基礎骨架（含註解說明各段用途）
- [x] 4.2 於 `buildTypes.getByName("release")` 設定 `isMinifyEnabled = true`、`isShrinkResources = true`，並設定 `proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")`
- [x] 4.3 確認 `benchmark` build type（`initWith(buildTypes.getByName("release"))`）在調整後仍維持 `signingConfig = signingConfigs.getByName("debug")` 且行為不受影響

## 5. 建置與功能驗證

- [x] 5.1 執行 `./gradlew :androidApp:assembleRelease`（Windows: `.\gradlew.bat :androidApp:assembleRelease`），確認建置成功且產出已簽章 APK
- [x] 5.2 安裝混淆後的 release APK 至實體裝置，手動操作首頁列表、詳情頁、收藏，確認皆正常運作、無因缺少 keep 規則導致的閃退或注入失敗（依使用者指示略過搜尋、歷史、設定頁面的 UI 測試）
- [x] 5.3 步驟 5.2 未發現因混淆導致的例外，`proguard-rules.pro` 維持基礎骨架即可

## 6. Lint 與最終驗證

- [x] 6.1 執行 `./gradlew ktlintCheck` 確認通過
- [x] 6.2 執行 `openspec validate android-release-signing --type change --strict --no-interactive` 確認 change 文件通過驗證
