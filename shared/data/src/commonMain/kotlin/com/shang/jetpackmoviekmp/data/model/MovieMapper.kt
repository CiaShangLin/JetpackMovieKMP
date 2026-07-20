package com.shang.jetpackmoviekmp.data.model

import com.shang.jetpackmoviekmp.database.entity.MovieCollectEntity
import com.shang.jetpackmoviekmp.database.entity.MovieHistoryEntity
import com.shang.jetpackmoviekmp.model.MovieCardResult

/**
 * 轉換為 [MovieCollectEntity]，供收藏寫入使用。
 *
 * 欄位一對一對應，`id` 沿用 [MovieCardResult.id]（作為 [MovieCollectEntity] 的 primary key）。
 *
 * @return 對應的 [MovieCollectEntity]，可直接傳給 `MovieCollectDao.insertMovieCollect`。
 */
fun MovieCardResult.asCollectEntity(): MovieCollectEntity {
    return MovieCollectEntity(
        id = id,
        title = title,
        posterPath = posterPath,
        voteAverage = voteAverage,
        releaseDate = releaseDate,
        timestamp = timestamp,
    )
}

/**
 * 轉換為 [MovieHistoryEntity]，供瀏覽紀錄寫入使用。
 *
 * 欄位一對一對應，`id` 沿用 [MovieCardResult.id]（作為 [MovieHistoryEntity] 的 primary key）。
 *
 * @return 對應的 [MovieHistoryEntity]，可直接傳給 `MovieHistoryDao.insertMovie`。
 */
fun MovieCardResult.asHistoryEntity(): MovieHistoryEntity {
    return MovieHistoryEntity(
        id = id,
        title = title,
        posterPath = posterPath,
        voteAverage = voteAverage,
        releaseDate = releaseDate,
        timestamp = timestamp,
    )
}
