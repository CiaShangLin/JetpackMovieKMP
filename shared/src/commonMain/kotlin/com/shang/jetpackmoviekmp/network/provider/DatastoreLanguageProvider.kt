package com.shang.jetpackmoviekmp.network.provider

import com.shang.jetpackmoviekmp.datastore.UserPreferenceDataSource
import com.shang.jetpackmoviekmp.model.LanguageMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.concurrent.Volatile

private const val DEFAULT_LANGUAGE_CODE = "zh-TW"

/**
 * 由持久化使用者偏好設定支援的 [LanguageProvider]。
 *
 * `getLanguageCode()` 會被 Ktor 的 `defaultRequest` 同步呼叫，因此這裡改在 [scope]
 * 中收集 [UserPreferenceDataSource.userData]，把最新語言代碼快取起來，而不是每次
 * request 都同步讀取 DataStore。在收集到第一筆資料前，會先回傳 [DEFAULT_LANGUAGE_CODE]
 * 作為安全的預設值。
 *
 * @param userPreferenceDataSource 語言偏好設定的資料來源。
 * @param scope 用來收集 [UserPreferenceDataSource.userData] 的 coroutine scope，
 * 生命週期建議與應用程式（application-level）一致。
 */
class DatastoreLanguageProvider(
    userPreferenceDataSource: UserPreferenceDataSource,
    scope: CoroutineScope,
) : LanguageProvider {

    @Volatile
    private var cachedLanguageCode: String = DEFAULT_LANGUAGE_CODE

    init {
        userPreferenceDataSource.userData
            .onEach { userData -> cachedLanguageCode = userData.languageMode.toLanguageCode() }
            .launchIn(scope)
    }

    override fun getLanguageCode(): String = cachedLanguageCode
}

internal fun LanguageMode.toLanguageCode(): String = when (this) {
    LanguageMode.ENGLISH -> "en-US"
    LanguageMode.TRADITIONAL_CHINESE -> "zh-TW"
    LanguageMode.SYSTEM_DEFAULT -> currentSystemLanguageCode() ?: DEFAULT_LANGUAGE_CODE
}
