package com.shang.jetpackmoviekmp.network.model

import com.shang.jetpackmoviekmp.model.MovieGenreBean
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MovieGenreResponse(
    @SerialName("genres")
    val genres: List<Genre> = listOf(),
) {
    /**
     * 電影類型
     * @param id 類型ID
     * @param name 類型名稱
     */
    @Serializable
    data class Genre(
        @SerialName("id")
        val id: Int = 0,
        @SerialName("name")
        val name: String = "",
    )
}

fun MovieGenreResponse.asExternalModel(): MovieGenreBean {
    return MovieGenreBean(
        genres = genres.map { MovieGenreBean.MovieGenre(it.id, it.name) },
    )
}
