package com.shang.jetpackmoviekmp.network.provider

import com.shang.jetpackmoviekmp.common.BaseHostUrlProvider
import com.shang.jetpackmoviekmp.datastore.UserPreferenceDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlin.concurrent.Volatile

private const val DEFAULT_BASE_HOST_URL = ""

/**
 * 由持久化使用者偏好設定支援的 [BaseHostUrlProvider]。
 *
 * `getBaseHostUrl()` 可能被圖片載入邏輯同步呼叫，因此這裡改在 [scope] 中收集
 * `UserPreferenceDataSource.userData` 的 `configuration.images.baseUrl`，把最新值快取起來，
 * 而不是每次呼叫都同步讀取 DataStore。在收集到第一筆資料前，會先回傳空字串。
 *
 * @param userPreferenceDataSource configuration 偏好設定的資料來源。
 * @param scope 用來收集 [UserPreferenceDataSource.userData] 的 coroutine scope，
 * 生命週期建議與應用程式（application-level）一致。
 */
class DatastoreBaseHostUrlProvider(
    userPreferenceDataSource: UserPreferenceDataSource,
    scope: CoroutineScope,
) : BaseHostUrlProvider {

    @Volatile
    private var cachedBaseHostUrl: String = DEFAULT_BASE_HOST_URL

    init {
        userPreferenceDataSource.userData
            .map { it.configuration.images.baseUrl }
            .distinctUntilChanged()
            .onEach { baseUrl -> cachedBaseHostUrl = baseUrl.normalizeBaseHostUrl() }
            .launchIn(scope)
    }

    override fun getBaseHostUrl(): String = cachedBaseHostUrl
}

private fun String.normalizeBaseHostUrl(): String =
    if (isNotEmpty() && !endsWith("/")) "$this/" else this
