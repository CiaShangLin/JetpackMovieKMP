package com.shang.jetpackmoviekmp.domain.usecase

import com.shang.jetpackmoviekmp.common.AppResult
import com.shang.jetpackmoviekmp.common.toAppError
import com.shang.jetpackmoviekmp.data.repository.MovieRepository
import com.shang.jetpackmoviekmp.data.repository.UserDataRepository
import com.shang.jetpackmoviekmp.model.ConfigurationBean
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.transform

/**
 * 取得 TMDB configuration，API 失敗時退回本地快取。
 *
 * @property movieRepository 電影資料來源 Repository，提供遠端 configuration 查詢
 * @property userDataRepository 使用者偏好設定 Repository，用來讀寫本地快取的 configuration
 * @param ioDispatcher 執行 IO 操作用的 dispatcher；正式環境由 `domainModule()` 透過
 *   `commonModule()` 提供的 `CommonDispatcher.IO` qualifier 注入，測試可直接建構並傳入
 *   test dispatcher，不需要透過 Koin。
 */
class GetConfigurationUseCase(
    private val movieRepository: MovieRepository,
    private val userDataRepository: UserDataRepository,
    private val ioDispatcher: CoroutineDispatcher,
) {

    /**
     * 取得 configuration，並依成功／失敗決定是否退回本地快取。
     *
     * 成功時將結果寫入本地快取後回傳；失敗時若本地已有快取的 configuration，
     * 視為成功回傳快取內容，皆無快取才回傳原始錯誤。
     *
     * 這裡是實際會被 iOS 端透過 `KoinHelper` 呼叫的邊界，因此把 [MovieRepository] 內部
     * 使用的 `kotlin.Result<T>` 轉換為可被 SKIE 明確匯出成 Swift enum 的 [AppResult]；
     * [MovieRepository] 本身不需要跟著改動。
     *
     * @return configuration 的 [Flow]，成功（含退回快取的情況）為 [AppResult.Success]，
     *   失敗且無快取為 [AppResult.Failure]
     */
    operator fun invoke(): Flow<AppResult<ConfigurationBean>> =
        // 從 repository 發出 Configuration 的遠端請求
        movieRepository.getConfiguration()
            .transform { result ->
                result.fold(
                    onSuccess = {
                        // API 成功，將設定資料寫入本地快取（DataStore）
                        userDataRepository.setConfiguration(it)
                        emit(AppResult.Success(it))
                    },
                    onFailure = {
                        val config = userDataRepository.userData.firstOrNull()?.configuration
                        if (config != null) {
                            // 若有快取資料，則回傳 Success 狀態給 UI
                            emit(AppResult.Success(config))
                        } else {
                            // 若沒有快取資料，則回傳原本的錯誤
                            emit(AppResult.Failure(it.toAppError()))
                        }
                    },
                )
            }.flowOn(ioDispatcher)
}
