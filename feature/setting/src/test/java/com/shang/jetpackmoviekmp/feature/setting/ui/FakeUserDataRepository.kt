package com.shang.jetpackmoviekmp.feature.setting.ui

import com.shang.jetpackmoviekmp.data.repository.UserDataRepository
import com.shang.jetpackmoviekmp.model.ConfigurationBean
import com.shang.jetpackmoviekmp.model.LanguageMode
import com.shang.jetpackmoviekmp.model.ThemeMode
import com.shang.jetpackmoviekmp.model.UserData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.CountDownLatch

internal class FakeUserDataRepository(
    initialUserData: UserData = UserData.getDefault(),
) : UserDataRepository {

    private val userDataFlow = MutableStateFlow(initialUserData)

    var setThemeModeCallCount = 0
        private set
    var setLanguageModeCallCount = 0
        private set
    var lastThemeMode: ThemeMode? = null
        private set
    var lastLanguageMode: LanguageMode? = null
        private set
    val setThemeModeLatch = CountDownLatch(1)
    val setLanguageModeLatch = CountDownLatch(1)

    override val userData: Flow<UserData> = userDataFlow

    override suspend fun setConfiguration(configuration: ConfigurationBean) = Unit

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        setThemeModeCallCount++
        lastThemeMode = themeMode
        setThemeModeLatch.countDown()
    }

    override suspend fun setLanguageMode(languageMode: LanguageMode) {
        setLanguageModeCallCount++
        lastLanguageMode = languageMode
        setLanguageModeLatch.countDown()
    }
}
