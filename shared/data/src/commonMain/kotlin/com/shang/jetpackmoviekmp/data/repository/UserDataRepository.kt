package com.shang.jetpackmoviekmp.data.repository

import com.shang.jetpackmoviekmp.model.ConfigurationBean
import com.shang.jetpackmoviekmp.model.LanguageMode
import com.shang.jetpackmoviekmp.model.ThemeMode
import com.shang.jetpackmoviekmp.model.UserData
import kotlinx.coroutines.flow.Flow

/**
 * 使用者偏好設定存取介面。
 *
 * 實作由 [UserDataRepositoryImpl] 提供。
 *
 * @see UserDataRepositoryImpl
 */
interface UserDataRepository {

    /**
     * 目前使用者偏好設定的 [Flow]，任何一次 [setConfiguration]／[setThemeMode]／[setLanguageMode]
     * 呼叫都會使後續 emission 反映最新值。
     */
    val userData: Flow<UserData>

    /**
     * 持久化 TMDB configuration（changeKeys、images 等）。
     *
     * @param configuration 欲持久化的 configuration 資料。
     */
    suspend fun setConfiguration(configuration: ConfigurationBean)

    /**
     * 持久化使用者選擇的主題模式。
     *
     * @param themeMode 欲持久化的主題模式。
     */
    suspend fun setThemeMode(themeMode: ThemeMode)

    /**
     * 持久化使用者選擇的語言模式。
     *
     * @param languageMode 欲持久化的語言模式。
     */
    suspend fun setLanguageMode(languageMode: LanguageMode)
}
