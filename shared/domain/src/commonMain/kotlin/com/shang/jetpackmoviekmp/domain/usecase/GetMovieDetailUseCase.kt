package com.shang.jetpackmoviekmp.domain.usecase

import com.shang.jetpackmoviekmp.data.repository.MovieRepository
import com.shang.jetpackmoviekmp.model.MovieDetailBean
import com.shang.jetpackmoviekmp.model.asMovieCardResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach

/**
 * 取得電影詳情，成功時額外寫入瀏覽紀錄。
 *
 * @property movieRepository 電影資料來源 Repository，提供電影詳情查詢與瀏覽紀錄寫入
 * @param ioDispatcher 執行 IO 操作用的 dispatcher；注入方式同 [GetConfigurationUseCase]。
 */
class GetMovieDetailUseCase(
    private val movieRepository: MovieRepository,
    private val ioDispatcher: CoroutineDispatcher,
) {

    /**
     * 取得指定電影的詳情，成功時額外寫入一筆瀏覽紀錄。
     *
     * @param movieId TMDB 電影 id
     * @return 電影詳情的 [Flow]，成功為 [Result.success]，失敗為 [Result.failure]
     */
    operator fun invoke(movieId: Int): Flow<Result<MovieDetailBean>> {
        return movieRepository.getMovieDetail(movieId)
            .onEach { result ->
                if (result.isSuccess) {
                    result.getOrNull()?.let {
                        try {
                            // 拿到Api資料時寫入歷史紀錄；寫入失敗不影響主流程（詳情已成功取得），故意忽略，
                            // 但必須先重新拋出 CancellationException，避免吞掉協程取消信號
                            // （比照 migrate-data-to-commonmain 的 PagingSource CancellationException 修正）
                            movieRepository.insertMovieHistory(it.asMovieCardResult())
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            // 忽略：瀏覽紀錄寫入失敗不應該讓已成功取得的電影詳情跟著失敗
                        }
                    }
                }
            }.flowOn(ioDispatcher)
    }
}
