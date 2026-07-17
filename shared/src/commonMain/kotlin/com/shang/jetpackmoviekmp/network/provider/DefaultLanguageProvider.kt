package com.shang.jetpackmoviekmp.network.provider

import com.shang.jetpackmoviekmp.common.LanguageProvider

/**
 * Default [LanguageProvider] used before user language persistence is introduced.
 */
class DefaultLanguageProvider : LanguageProvider {
    override fun getLanguageCode(): String = "zh-TW"
}
