package com.shang.jetpackmoviekmp.domain.usecase

import com.shang.jetpackmoviekmp.data.repository.MovieRepository
import com.shang.jetpackmoviekmp.model.MovieCardResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn

/**
 * 取得瀏覽紀錄清單，並標記每部電影目前的收藏狀態。
 *
 * @property movieRepository 電影資料來源 Repository，提供瀏覽紀錄與收藏 id 查詢
 * @param ioDispatcher 執行 IO 操作用的 dispatcher；注入方式同 [GetConfigurationUseCase]。
 */
class GetHistoryMovieListUseCase(
    private val movieRepository: MovieRepository,
    private val ioDispatcher: CoroutineDispatcher,
) {
    /**
     * 合併瀏覽紀錄與收藏 id 清單，標記每部電影目前是否已收藏。
     *
     * @return 瀏覽紀錄的 [Flow]，每筆 [MovieCardResult.isCollect] 反映目前實際收藏狀態
     */
    operator fun invoke(): Flow<List<MovieCardResult>> {
        val allMovieHistory = movieRepository.getAllMovieHistory()
        val collectIds = movieRepository.getCollectedMovieIds()
        return combine(allMovieHistory, collectIds) { history, collectIds ->
            history.map { movie ->
                movie.copy(
                    isCollect = collectIds.contains(movie.id),
                )
            }
        }.flowOn(ioDispatcher)
    }
}
