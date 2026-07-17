package com.shang.jetpackmoviekmp.model

import kotlinx.serialization.Serializable

/**
 * 資料Api參數設置
 */
@Serializable
data class ConfigurationBean(
    val changeKeys: List<String> = emptyList(),
    val images: Images = Images(),
) {
    @Serializable
    data class Images(
        val backdropSizes: List<String> = emptyList(),
        val baseUrl: String = "",
        val logoSizes: List<String> = emptyList(),
        val posterSizes: List<String> = emptyList(),
        val profileSizes: List<String> = emptyList(),
        val secureBaseUrl: String = "",
        val stillSizes: List<String> = emptyList(),
    )
}
