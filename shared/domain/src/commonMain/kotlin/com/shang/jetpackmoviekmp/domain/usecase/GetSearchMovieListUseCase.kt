package com.shang.jetpackmoviekmp.domain.usecase

import androidx.paging.PagingData
import androidx.paging.map
import com.shang.jetpackmoviekmp.data.repository.MovieRepository
import com.shang.jetpackmoviekmp.model.MovieCardResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/**
 * 取得搜尋結果，並以目前收藏電影 id 標記每筆分頁資料的收藏狀態。
 *
 * @property movieRepository 提供搜尋 Pager 與收藏電影 id 資料流。
 * @property ioDispatcher 執行資料存取的協程 Dispatcher。
 */
class GetSearchMovieListUseCase(
    private val movieRepository: MovieRepository,
    private val ioDispatcher: CoroutineDispatcher,
) {
    /**
     * 建立指定關鍵字的搜尋分頁資料流。
     *
     * @param query 要查詢的電影名稱。
     * @return 會隨收藏資料異動更新 `isCollect` 的分頁資料流。
     */
    operator fun invoke(query: String): Flow<PagingData<MovieCardResult>> {
        val pageFlow = movieRepository.getMovieSearchPager(query)
            .flowOn(ioDispatcher)

        // 取得已收藏電影 id 清單，轉為 Set 以提升 contains 查詢效能
        val collectIdsFlow = movieRepository.getCollectedMovieIds()
            .map { it.toSet() }
            .flowOn(ioDispatcher)

        return pageFlow.combine(collectIdsFlow) { pagingData, collectIds ->
            pagingData.map { movie ->
                movie.copy(isCollect = collectIds.contains(movie.id))
            }
        }
    }
}
