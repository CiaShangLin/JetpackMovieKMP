import Shared

/// 收藏切換相關測試共用的最小電影卡片 fixture，供多個 ViewModel／`MovieCollectToggler` 測試共用，
/// 避免每個測試檔各自重複同一份建構邏輯。
func makeMovieCardResult(id: Int32, isCollect: Bool = false) -> MovieCardResult {
    MovieCardResult(
        adult: false,
        backdropPath: "",
        genreIds: [],
        id: id,
        originalLanguage: "en",
        originalTitle: "Movie \(id)",
        overview: "",
        popularity: 0,
        posterPath: "",
        releaseDate: "2026-01-01",
        title: "Movie \(id)",
        video: false,
        voteAverage: 0,
        voteCount: 0,
        isCollect: isCollect,
        timestamp: 0
    )
}

func makeMovieCardData(id: Int32, isCollect: Bool) -> MovieCardData {
    MovieCardData(
        movieCardId: id,
        movieCardTitle: "Movie \(id)",
        movieCardPosterPath: "",
        movieCardReleaseDate: "2026-01-01",
        movieCardVoteAverage: 0,
        movieCardIsCollect: isCollect,
        movieCardTimestamp: 0
    )
}
