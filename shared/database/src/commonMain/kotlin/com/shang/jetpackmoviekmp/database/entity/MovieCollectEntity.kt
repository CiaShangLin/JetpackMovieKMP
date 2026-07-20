package com.shang.jetpackmoviekmp.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.shang.jetpackmoviekmp.model.MovieCardResult

/**
 * 使用者收藏的電影，對應資料庫 `MovieCollectEntity` table 的一列。
 */
@Entity(tableName = "MovieCollectEntity")
data class MovieCollectEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "poster_path")
    val posterPath: String,

    @ColumnInfo(name = "vote_average")
    val voteAverage: Double,

    @ColumnInfo(name = "release_date")
    val releaseDate: String,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,
)

/**
 * 轉換為 [MovieCardResult]，並固定標記為已收藏（[MovieCardResult.isCollect] = true）。
 */
fun MovieCollectEntity.asExtendedModel(): MovieCardResult {
    return MovieCardResult(
        id = id,
        title = title,
        posterPath = posterPath,
        voteAverage = voteAverage,
        releaseDate = releaseDate,
        timestamp = timestamp,
        isCollect = true,
    )
}
