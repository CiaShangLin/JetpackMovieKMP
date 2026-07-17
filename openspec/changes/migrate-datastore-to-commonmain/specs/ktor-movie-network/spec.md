## MODIFIED Requirements

### Requirement: Request query parameters 包含 api_key 與 datastore-backed language

`HttpClient` request configuration 必須在每個 TMDB request 附加 `api_key`，並必須從 DI-provided `LanguageProvider` 附加 `language`。在 production DI 中，`LanguageProvider` 必須由 user preferences datastore 提供，而不是固定 default provider。

#### Scenario: 每個 request 都包含 api_key

- **WHEN** `MovieDataSource` 呼叫任一 TMDB endpoint
- **THEN** outgoing request 包含 `api_key` query parameter

#### Scenario: language 來自 datastore-backed provider

- **WHEN** `UserPreferenceDataSource` 持久化 `LanguageMode.ENGLISH`
- **THEN** 後續 network request 包含 `language=en-US`

#### Scenario: language 可跨 request 變更

- **WHEN** persisted language 從 English 變更為 Traditional Chinese
- **THEN** 後續 network requests 使用 `language=zh-TW`，且不需要重新建立 `MovieDataSource`
