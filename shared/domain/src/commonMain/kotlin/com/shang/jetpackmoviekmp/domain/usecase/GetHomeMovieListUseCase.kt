package com.shang.jetpackmoviekmp.domain.usecase

import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.shang.jetpackmoviekmp.data.repository.MovieRepository
import com.shang.jetpackmoviekmp.model.MovieCardResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/**
 * 取得首頁電影清單 UseCase
 *
 * 1. 從 [MovieRepository] 取得指定類型的電影分頁資料流
 * 2. 取得已收藏電影 id 清單，並標記每部電影的收藏狀態
 * 3. 支援依賴注入與 IO Dispatcher 切換，確保效能與可測試性
 *
 * @param movieRepository 電影資料來源 Repository
 * @param ioDispatcher 執行 IO 操作的協程 Dispatcher
 */
class GetHomeMovieListUseCase(
    private val movieRepository: MovieRepository,
    private val ioDispatcher: CoroutineDispatcher,
) {

    /**
     * 取得首頁電影分頁資料流，並標記每部電影是否已收藏
     *
     * @param withGenres 指定查詢的電影類型 id 字串
     * @param scope 提供 [cachedIn] 快取的 [CoroutineScope]；`commonMain` 不假定呼叫端一定是
     *   Android `ViewModel`，交由呼叫端決定實際生命週期
     * @return Flow<PagingData<MovieCardResult>> 分頁資料流，已標記 isCollect 狀態
     */
    operator fun invoke(withGenres: String, scope: CoroutineScope): Flow<PagingData<MovieCardResult>> {
        // 分頁本身用 cachedIn 快取，讓多個 collector 共用同一份分頁資料
        val pagerFlow = movieRepository.getMovieListPager(withGenres)
            .flowOn(ioDispatcher)
            .cachedIn(scope)

        // 取得已收藏電影 id 清單，轉為 Set 以提升 contains 查詢效能
        val collectIdsFlow = movieRepository.getCollectedMovieIds()
            .map { it.toSet() }
            .flowOn(ioDispatcher)

        // 合併分頁資料與收藏 id，標記每部電影的 isCollect 狀態
        return pagerFlow
            .combine(collectIdsFlow) { pagingData, collectIds ->
                pagingData.map { movie ->
                    movie.copy(isCollect = collectIds.contains(movie.id))
                }
            }
    }
}
