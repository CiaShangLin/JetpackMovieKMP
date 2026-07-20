package com.shang.jetpackmoviekmp.core.ui

import com.shang.jetpackmoviekmp.model.MovieCardResult

/**
 * 電影卡片元件顯示所需的 UI 資料。
 *
 * @property movieCardId 電影唯一識別碼。
 * @property movieCardTitle 電影標題。
 * @property movieCardPosterPath 電影海報圖片路徑，可為相對 TMDB 圖片路徑或完整 URL。
 * @property movieCardReleaseDate 電影上映日期文字。
 * @property movieCardVoteAverage 電影平均評分。
 * @property movieCardIsCollect 使用者是否已收藏此電影。
 * @property movieCardTimestamp 收藏或紀錄時間戳。
 */
data class MovieCardData(
    val movieCardId: Int,
    val movieCardTitle: String,
    val movieCardPosterPath: String,
    val movieCardReleaseDate: String,
    val movieCardVoteAverage: Double,
    val movieCardIsCollect: Boolean,
    val movieCardTimestamp: Long,
)

/**
 * 將 UI 層 [MovieCardData] 轉回 shared model 的 [MovieCardResult]。
 *
 * @return 對應的跨平台電影卡片資料。
 */
fun MovieCardData.asMovieCardResult(): MovieCardResult =
    MovieCardResult(
        id = movieCardId,
        title = movieCardTitle,
        posterPath = movieCardPosterPath,
        releaseDate = movieCardReleaseDate,
        voteAverage = movieCardVoteAverage,
        isCollect = movieCardIsCollect,
        timestamp = movieCardTimestamp,
    )

/**
 * 將 shared model 的 [MovieCardResult] 轉為 Android UI card 所需資料。
 *
 * @return 對應的 [MovieCardData]。
 */
fun MovieCardResult.asMovieCardData(): MovieCardData =
    MovieCardData(
        movieCardId = id,
        movieCardTitle = title,
        movieCardPosterPath = posterPath,
        movieCardReleaseDate = releaseDate,
        movieCardVoteAverage = voteAverage,
        movieCardIsCollect = isCollect,
        movieCardTimestamp = timestamp,
    )
