package com.shang.jetpackmoviekmp

import com.shang.jetpackmoviekmp.domain.usecase.GetConfigurationUseCase
import com.shang.jetpackmoviekmp.model.ConfigurationBean
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Swift-friendly wrapper for loading configuration.
 *
 * Kotlin `Result<T>` is exported to Swift as an opaque boxed value, so iOS UI code should
 * consume this explicit state type instead of trying to unwrap `Result<ConfigurationBean>`.
 */
class IosConfigurationLoader(
    private val getConfigurationUseCase: GetConfigurationUseCase,
) {

    fun invoke(): Flow<IosConfigurationLoadState> {
        return getConfigurationUseCase().map { result ->
            result.fold(
                onSuccess = { IosConfigurationLoadState.Success(it) },
                onFailure = { IosConfigurationLoadState.Failure(it.message ?: "載入失敗，請檢查網路連線") },
            )
        }
    }
}

sealed interface IosConfigurationLoadState {

    data class Success(
        val data: ConfigurationBean,
    ) : IosConfigurationLoadState

    data class Failure(
        val message: String,
    ) : IosConfigurationLoadState
}
