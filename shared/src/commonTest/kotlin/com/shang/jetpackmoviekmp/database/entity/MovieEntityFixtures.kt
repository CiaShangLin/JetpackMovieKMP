package com.shang.jetpackmoviekmp.database.entity

/**
 * 測試用的收藏電影 fixture，供 DAO／Koin module 讀寫測試共用一致的樣本資料。
 */
internal fun testMovieCollectEntity(id: Int = 1) = MovieCollectEntity(
    id = id,
    title = "Inception",
    posterPath = "/poster.jpg",
    voteAverage = 8.8,
    releaseDate = "2010-07-16",
    timestamp = 1_000L,
)
