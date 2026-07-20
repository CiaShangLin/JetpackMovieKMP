package com.shang.jetpackmoviekmp

import com.shang.jetpackmoviekmp.data.repository.UserDataRepository
import com.shang.jetpackmoviekmp.domain.usecase.GetConfigurationUseCase
import com.shang.jetpackmoviekmp.model.LanguageMode
import kotlinx.coroutines.flow.first

/**
 * Android 驗證畫面使用的 app facade。
 *
 * 對外只暴露簡單的字串與布林參數，避免 `androidApp` 直接依賴 data/domain/model
 * 等底層 module 型別。實例由 `shared:app` 的 Koin 組裝根建立。
 */
class AppDiagnostics internal constructor(
    private val getConfigurationUseCase: GetConfigurationUseCase,
    private val userDataRepository: UserDataRepository,
) {
    /**
     * 載入 configuration 並回傳適合顯示於驗證畫面的摘要字串。
     */
    suspend fun loadConfigurationSummary(): String = getConfigurationUseCase().first().toString()

    /**
     * 持久化語言模式並回傳已套用的語言名稱。
     *
     * @param useEnglish 為 `true` 時套用英文，否則套用繁體中文。
     */
    suspend fun setLanguage(useEnglish: Boolean): String {
        val languageMode = if (useEnglish) {
            LanguageMode.ENGLISH
        } else {
            LanguageMode.TRADITIONAL_CHINESE
        }
        userDataRepository.setLanguageMode(languageMode)
        return languageMode.toString()
    }
}
