package com.shang.jetpackmoviekmp.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.shang.jetpackmoviekmp.database.entity.MovieHistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * 電影瀏覽紀錄的資料存取介面。
 */
@Dao
interface MovieHistoryDao {

    /**
     * 取得所有瀏覽紀錄，資料變動時會重新發出。
     */
    @Query("SELECT * FROM MovieHistoryEntity")
    fun getAllMovies(): Flow<List<MovieHistoryEntity>>

    /**
     * 新增或覆蓋一筆瀏覽紀錄（id 衝突時取代既有資料）。
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovie(movie: MovieHistoryEntity)

    /**
     * 移除一筆瀏覽紀錄。
     */
    @Delete
    suspend fun deleteMovie(movie: MovieHistoryEntity)

    /**
     * 清空所有瀏覽紀錄，回傳實際刪除的筆數。
     */
    @Query("DELETE FROM MovieHistoryEntity")
    suspend fun deleteAllMovies(): Int
}
