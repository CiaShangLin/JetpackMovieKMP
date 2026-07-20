package com.shang.jetpackmoviekmp.domain.usecase

import androidx.paging.PagingData
import com.shang.jetpackmoviekmp.domain.FakeMovieRepository
import com.shang.jetpackmoviekmp.model.MovieCardResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * 分頁邏輯本身比照 `data` 層既有作法（`MovieRepositoryImplTest.getMovieListPager_is_collectible_without_throwing`）
 * 做 collectible smoke test，不斷言 [PagingData] 實際內容——專案未引入 `androidx.paging:paging-testing`，
 * 無法在 commonTest 直接用 `asSnapshot()` 斷言分頁內容（見 design.md 風險章節）。
 */
class GetHomeMovieListUseCaseTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun invoke_is_collectible_without_throwing() = runTest {
        val movieRepository = FakeMovieRepository().apply {
            movieListPager = flowOf(PagingData.from(listOf(MovieCardResult(id = 1, title = "A"))))
            collectedMovieIdsFlow.value = listOf(1)
        }
        val useCase = GetHomeMovieListUseCase(
            movieRepository = movieRepository,
            ioDispatcher = UnconfinedTestDispatcher(testScheduler),
        )

        // `cachedIn` 會在傳入的 CoroutineScope 上啟動一個常駐 job 持續服務後續 collector，
        // 不會隨著 collect 到第一筆資料就結束；用 runTest 的 `this` 當 scope 會讓
        // 該 job 在測試結束時仍未完成（`UncompletedCoroutinesError`）。改用 `backgroundScope`
        // ——它是 kotlinx-coroutines-test 專為這種「測試不需要等待其結束」的背景工作設計，
        // 會在測試結束時自動取消，不需要手動 cancel。
        val pagingData = useCase.invoke(withGenres = "28", scope = backgroundScope).first()

        assertNotNull(pagingData)
    }
}
