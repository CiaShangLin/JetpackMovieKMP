package com.shang.jetpackmoviekmp.datastore

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.shang.jetpackmoviekmp.model.ConfigurationBean
import com.shang.jetpackmoviekmp.model.LanguageMode
import com.shang.jetpackmoviekmp.model.ThemeMode
import com.shang.jetpackmoviekmp.model.UserData
import com.shang.jetpackmoviekmp.sharedJson

internal object UserPreferencesKeys {
    val CONFIGURATION_JSON = stringPreferencesKey("configuration_json")
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val LANGUAGE_MODE = stringPreferencesKey("language_mode")
}

/** 讀取 [key] 並用 [parse] 解析；缺值或解析失敗一律回退到 [default]。 */
private inline fun <T> Preferences.readOrDefault(
    key: Preferences.Key<String>,
    default: T,
    parse: (String) -> T,
): T = this[key]?.let { runCatching { parse(it) }.getOrNull() } ?: default

internal fun Preferences.toUserData(): UserData {
    val default = UserData.getDefault()
    return UserData(
        configuration = readOrDefault(UserPreferencesKeys.CONFIGURATION_JSON, default.configuration) {
            sharedJson.decodeFromString(ConfigurationBean.serializer(), it)
        },
        themeMode = readOrDefault(UserPreferencesKeys.THEME_MODE, default.themeMode, ThemeMode::valueOf),
        languageMode = readOrDefault(UserPreferencesKeys.LANGUAGE_MODE, default.languageMode, LanguageMode::valueOf),
    )
}
