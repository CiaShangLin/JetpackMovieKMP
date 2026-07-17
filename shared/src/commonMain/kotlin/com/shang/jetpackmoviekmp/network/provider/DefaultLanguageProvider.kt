package com.shang.jetpackmoviekmp.network.provider

/**
 * Default [LanguageProvider] used before user language persistence is introduced.
 */
class DefaultLanguageProvider : LanguageProvider {
    override fun getLanguageCode(): String = "zh-TW"
}
