package com.shang.jetpackmoviekmp.domain.usecase

import com.shang.jetpackmoviekmp.data.repository.MovieRepository
import com.shang.jetpackmoviekmp.model.MovieCardResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/**
 * 取得電影推薦清單，並標記每部電影目前的收藏狀態。
 *
 * @property movieRepository 電影資料來源 Repository，提供推薦清單與收藏 id 查詢
 * @param ioDispatcher 執行 IO 操作用的 dispatcher；注入方式同 [GetConfigurationUseCase]。
 */
class GetMovieRecommendUseCase(
    private val movieRepository: MovieRepository,
    private val ioDispatcher: CoroutineDispatcher,
) {

    /**
     * 取得指定電影的推薦清單，並標記每部推薦電影目前是否已收藏。
     *
     * @param movieId TMDB 電影 id
     * @return 推薦清單的 [Flow]，成功為 [Result.success]（每部電影的 `isCollect`
     *   已反映目前實際收藏狀態），失敗為 [Result.failure]
     */
    operator fun invoke(movieId: Int): Flow<Result<List<MovieCardResult>>> {
        val collectIdsFlow = movieRepository.getCollectedMovieIds()
            .map { it.toSet() }
        val movieRecommendationsFlow = movieRepository.getMovieRecommendations(movieId)
            .map {
                it.fold(
                    onSuccess = { Result.success(it.results) },
                    onFailure = { Result.failure(it) },
                )
            }

        // 單一終端 flowOn 即可涵蓋上游兩條 Flow，不需要在每條來源各自重複呼叫
        // （比照 [GetHistoryMovieListUseCase] 的寫法）
        return movieRecommendationsFlow.combine(collectIdsFlow) { recommendations, collectIds ->
            recommendations.mapCatching {
                it.map {
                    it.copy(isCollect = collectIds.contains(it.id))
                }
            }
        }.flowOn(ioDispatcher)
    }
}
