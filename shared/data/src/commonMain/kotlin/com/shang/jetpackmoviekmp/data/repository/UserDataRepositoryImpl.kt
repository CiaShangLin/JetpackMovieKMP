package com.shang.jetpackmoviekmp.data.repository

import com.shang.jetpackmoviekmp.datastore.UserPreferenceDataSource
import com.shang.jetpackmoviekmp.model.ConfigurationBean
import com.shang.jetpackmoviekmp.model.LanguageMode
import com.shang.jetpackmoviekmp.model.ThemeMode
import com.shang.jetpackmoviekmp.model.UserData
import kotlinx.coroutines.flow.Flow

/**
 * [UserDataRepository] 的預設實作，包裝 [UserPreferenceDataSource]。
 */
internal class UserDataRepositoryImpl(
    private val userPreferenceDataSource: UserPreferenceDataSource,
) : UserDataRepository {
    override val userData: Flow<UserData> = userPreferenceDataSource.userData

    override suspend fun setConfiguration(configuration: ConfigurationBean) {
        userPreferenceDataSource.setConfiguration(configuration)
    }

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        userPreferenceDataSource.setThemeMode(themeMode)
    }

    override suspend fun setLanguageMode(languageMode: LanguageMode) {
        userPreferenceDataSource.setLanguageMode(languageMode)
    }
}
