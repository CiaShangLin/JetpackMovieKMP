## MODIFIED Requirements

### Requirement: TMDB HttpClient 基底路徑設定
`shared/network` 的 `commonMain` MUST 透過 Ktor `HttpClient` 的 `defaultRequest` 設定 TMDB API 的基底路徑（`https://api.themoviedb.org/3/`），且各端點呼叫 MUST 使用不帶開頭斜線的相對路徑，讓實際發出的請求 URL 保留 `/3/` 前綴。

#### Scenario: 相對路徑正確解析成完整 URL
- **WHEN** `MovieDataSource` 以相對路徑（例如 `movie/{id}`）呼叫端點
- **THEN** 實際送出的請求 URL 為 `https://api.themoviedb.org/3/movie/{id}`，前綴未被覆蓋或遺漏

#### Scenario: 誤用開頭斜線會被測試攔截
- **WHEN** 開發者不慎以 `/movie/{id}`（開頭帶斜線）呼叫端點
- **THEN** `shared:network` 的 `commonTest` 使用 `ktor-client-mock` 驗證組出的完整 URL 時 MUST 能偵測到 `/3/` 前綴遺失並使測試失敗
