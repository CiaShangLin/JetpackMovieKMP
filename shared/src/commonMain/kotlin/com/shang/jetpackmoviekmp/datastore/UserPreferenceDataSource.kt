package com.shang.jetpackmoviekmp.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.shang.jetpackmoviekmp.model.ConfigurationBean
import com.shang.jetpackmoviekmp.model.LanguageMode
import com.shang.jetpackmoviekmp.model.ThemeMode
import com.shang.jetpackmoviekmp.model.UserData
import com.shang.jetpackmoviekmp.sharedJson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 在 shared KMP 程式碼中持久化使用者偏好設定（configuration、theme、language）。
 *
 * 底層使用 Preferences DataStore：`ConfigurationBean` 以 JSON 字串序列化後存放，
 * `themeMode`／`languageMode` 則以列舉名稱存放。尚未持久化任何值時，
 * [userData] 會以 [UserData.getDefault] 補齊缺漏欄位。
 *
 * @property dataStore 平台建立的 preferences DataStore（見 `createUserPreferencesDataStore`）。
 */
class UserPreferenceDataSource(
    private val dataStore: DataStore<Preferences>,
) {

    /**
     * 目前使用者偏好設定的 flow，任何一次 [setConfiguration]／[setThemeMode]／[setLanguageMode]
     * 呼叫都會使後續 emission 反映最新值。
     */
    val userData: Flow<UserData> = dataStore.data.map { it.toUserData() }

    /**
     * 持久化 TMDB configuration（changeKeys、images 等），以 JSON 字串儲存。
     *
     * @param configuration 欲持久化的 configuration 資料。
     */
    suspend fun setConfiguration(configuration: ConfigurationBean) {
        dataStore.edit { preferences ->
            preferences[UserPreferencesKeys.CONFIGURATION_JSON] =
                sharedJson.encodeToString(ConfigurationBean.serializer(), configuration)
        }
    }

    /**
     * 持久化使用者選擇的主題模式。
     *
     * @param themeMode 欲持久化的主題模式。
     */
    suspend fun setThemeMode(themeMode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[UserPreferencesKeys.THEME_MODE] = themeMode.name
        }
    }

    /**
     * 持久化使用者選擇的語言模式，供 [com.shang.jetpackmoviekmp.network.provider.DatastoreLanguageProvider]
     * 收集並反映到後續的 TMDB network request。
     *
     * @param languageMode 欲持久化的語言模式。
     */
    suspend fun setLanguageMode(languageMode: LanguageMode) {
        dataStore.edit { preferences ->
            preferences[UserPreferencesKeys.LANGUAGE_MODE] = languageMode.name
        }
    }
}
