## MODIFIED Requirements

### Requirement: Request query parameters include api_key and datastore-backed language

`HttpClient` request configuration MUST append `api_key` to every TMDB request and MUST append `language` from the DI-provided `LanguageProvider`. In production DI, `LanguageProvider` MUST be backed by user preferences datastore rather than a fixed default provider.

#### Scenario: every request includes api_key

- **WHEN** `MovieDataSource` calls any TMDB endpoint
- **THEN** the outgoing request contains the `api_key` query parameter

#### Scenario: language comes from datastore-backed provider

- **WHEN** `UserPreferenceDataSource` persists `LanguageMode.ENGLISH`
- **THEN** a later network request contains `language=en-US`

#### Scenario: language changes across requests

- **WHEN** the persisted language changes from English to Traditional Chinese
- **THEN** subsequent network requests use `language=zh-TW` without recreating `MovieDataSource`
