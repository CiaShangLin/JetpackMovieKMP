package com.shang.jetpackmoviekmp.data.repository

import androidx.paging.PagingData
import com.shang.jetpackmoviekmp.model.ConfigurationBean
import com.shang.jetpackmoviekmp.model.MovieCardResult
import com.shang.jetpackmoviekmp.model.MovieCastAndCrewBean
import com.shang.jetpackmoviekmp.model.MovieDetailBean
import com.shang.jetpackmoviekmp.model.MovieGenreBean
import com.shang.jetpackmoviekmp.model.MovieRecommendBean
import kotlinx.coroutines.flow.Flow

/**
 * 整合 network 與本地資料庫的電影資料存取介面。
 *
 * 電影相關的網路查詢（configuration、分頁列表／搜尋、詳情等）皆回傳可觀察的 [Flow]，
 * 收藏／瀏覽紀錄則額外提供讀寫本地資料庫的方法，實作由 [MovieRepositoryImpl] 提供。
 *
 * @see MovieRepositoryImpl
 */
interface MovieRepository {

    /**
     * 取得 TMDB configuration（圖片 base URL、可用尺寸等）。
     *
     * @return configuration 的 [Flow]，成功為 [Result.success]，失敗（network 錯誤）為 [Result.failure]。
     */
    fun getConfiguration(): Flow<Result<ConfigurationBean>>

    /**
     * 取得 TMDB 可用的電影類型清單。
     *
     * @return 電影類型的 [Flow]，成功為 [Result.success]，失敗（network 錯誤）為 [Result.failure]。
     */
    fun getMovieGenres(): Flow<Result<MovieGenreBean>>

    /**
     * 依電影類型分頁載入 discover 電影列表。
     *
     * @param withGenres TMDB 類型 id filter，對應 `with_genres` 查詢參數。
     * @return 電影卡片分頁資料的 [Flow]。
     */
    fun getMovieListPager(withGenres: String): Flow<PagingData<MovieCardResult>>

    /**
     * 依關鍵字分頁搜尋電影。
     *
     * @param query 搜尋關鍵字。
     * @return 搜尋結果分頁資料的 [Flow]。
     */
    fun getMovieSearchPager(query: String): Flow<PagingData<MovieCardResult>>

    /**
     * 取得指定電影的詳細資料。
     *
     * @param id TMDB 電影 id。
     * @return 電影詳情的 [Flow]，成功為 [Result.success]，失敗（network 錯誤）為 [Result.failure]。
     */
    fun getMovieDetail(id: Int): Flow<Result<MovieDetailBean>>

    /**
     * 取得指定電影的 TMDB 推薦清單。
     *
     * @param id TMDB 電影 id。
     * @return 推薦結果的 [Flow]，成功為 [Result.success]，失敗（network 錯誤）為 [Result.failure]。
     */
    fun getMovieRecommendations(id: Int): Flow<Result<MovieRecommendBean>>

    /**
     * 取得指定電影的演員與工作人員名單。
     *
     * @param id TMDB 電影 id。
     * @return 演職員資料的 [Flow]，成功為 [Result.success]，失敗（network 錯誤）為 [Result.failure]。
     */
    fun getMovieActor(id: Int): Flow<Result<MovieCastAndCrewBean>>

    // MovieCollectDao

    /**
     * 新增一筆電影收藏（依 [MovieCardResult.id] 覆蓋既有資料）。
     *
     * @param movieResult 欲收藏的電影。
     */
    suspend fun insertMovieCollect(movieResult: MovieCardResult)

    /**
     * 移除一筆電影收藏。
     *
     * @param movieResult 欲移除收藏的電影。
     */
    suspend fun deleteMovieCollect(movieResult: MovieCardResult)

    /**
     * 取得目前所有已收藏電影的 id 清單，資料變動時會重新發出。
     *
     * @return 已收藏電影 id 清單的 [Flow]。
     */
    fun getCollectedMovieIds(): Flow<List<Int>>

    /**
     * 取得目前所有已收藏電影，資料變動時會重新發出。
     *
     * @return 已收藏電影清單的 [Flow]（每筆 [MovieCardResult.isCollect] 恆為 `true`）。
     */
    fun getAllMovieCollect(): Flow<List<MovieCardResult>>

    /**
     * 依 id 查詢單一電影是否已收藏。
     *
     * @param id TMDB 電影 id。
     * @return 對應收藏資料的 [Flow]；尚未收藏時 emission 為 null。
     */
    fun getMovieCollectEntityById(id: Int): Flow<MovieCardResult?>

    // MovieHistoryDao

    /**
     * 新增一筆瀏覽紀錄（依 [MovieCardResult.id] 覆蓋既有資料）。
     *
     * @param movieResult 欲記錄瀏覽的電影。
     */
    suspend fun insertMovieHistory(movieResult: MovieCardResult)

    /**
     * 移除一筆瀏覽紀錄。
     *
     * @param movieResult 欲移除的瀏覽紀錄。
     */
    suspend fun deleteMovieHistory(movieResult: MovieCardResult)

    /**
     * 取得目前所有瀏覽紀錄，資料變動時會重新發出。
     *
     * @return 瀏覽紀錄清單的 [Flow]。
     */
    fun getAllMovieHistory(): Flow<List<MovieCardResult>>

    /**
     * 清空所有瀏覽紀錄。
     *
     * @return 是否有資料被刪除（原本即為空清單時回傳 `false`）。
     */
    suspend fun deleteAllMovieHistory(): Boolean
}
