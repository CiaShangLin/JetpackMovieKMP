import Shared

/// 開發階段用的假電影卡片資料，供 `MovieCardView` Preview／首頁畫面先行開發使用。
enum HomeMockData {
    static let movieCards: [MovieCardData] = [
        MovieCardData(
            movieCardId: 1,
            movieCardTitle: "Sample Movie One",
            movieCardPosterPath: "https://fastly.picsum.photos/id/1020/400/300.jpg",
            movieCardReleaseDate: "2023-10-01",
            movieCardVoteAverage: 8.7,
            movieCardIsCollect: false,
            movieCardTimestamp: 0
        ),
        MovieCardData(
            movieCardId: 2,
            movieCardTitle: "Sample Movie Two With A Longer Title",
            movieCardPosterPath: "https://fastly.picsum.photos/id/1021/400/300.jpg",
            movieCardReleaseDate: "2024-03-15",
            movieCardVoteAverage: 6.2,
            movieCardIsCollect: true,
            movieCardTimestamp: 0
        ),
        MovieCardData(
            movieCardId: 3,
            movieCardTitle: "Sample Movie Three",
            movieCardPosterPath: "https://fastly.picsum.photos/id/1022/400/300.jpg",
            movieCardReleaseDate: "2022-07-08",
            movieCardVoteAverage: 9.1,
            movieCardIsCollect: false,
            movieCardTimestamp: 0
        )
    ]

    /// 給 `HomeViewModel`／`HomeUiState` 假資料流程用的分類清單
    static let genres: [MovieGenreBean.MovieGenre] = [
        MovieGenreBean.MovieGenre(id: 28, name: "動作"),
        MovieGenreBean.MovieGenre(id: 35, name: "喜劇"),
        MovieGenreBean.MovieGenre(id: 18, name: "劇情")
    ]

    /// 給 `HomeViewModel`／`HomeUiState` 假資料流程用的分類電影清單（key 為分類 id）
    static let movies: [Int: [MovieCardResult]] = [
        28: [
            movie(
                id: 101,
                posterPath: "/poster1.jpg",
                releaseDate: "2023-10-01",
                title: "動作電影 A",
                voteAverage: 8.7,
                isCollect: false
            ),
            movie(
                id: 102,
                posterPath: "/poster2.jpg",
                releaseDate: "2024-01-12",
                title: "動作電影 B",
                voteAverage: 7.1,
                isCollect: true
            ),
            movie(
                id: 103,
                posterPath: "/poster3.jpg",
                releaseDate: "2022-05-20",
                title: "動作電影 C",
                voteAverage: 6.5,
                isCollect: false
            )
        ],
        35: [
            movie(
                id: 201,
                posterPath: "/poster4.jpg",
                releaseDate: "2023-11-03",
                title: "喜劇電影 A",
                voteAverage: 7.9,
                isCollect: false
            ),
            movie(
                id: 202,
                posterPath: "/poster5.jpg",
                releaseDate: "2024-02-14",
                title: "喜劇電影 B",
                voteAverage: 6.8,
                isCollect: true
            ),
            movie(
                id: 203,
                posterPath: "/poster6.jpg",
                releaseDate: "2021-09-30",
                title: "喜劇電影 C",
                voteAverage: 8.0,
                isCollect: false
            )
        ],
        18: [
            movie(
                id: 301,
                posterPath: "/poster7.jpg",
                releaseDate: "2023-06-18",
                title: "劇情電影 A",
                voteAverage: 9.1,
                isCollect: false
            ),
            movie(
                id: 302,
                posterPath: "/poster8.jpg",
                releaseDate: "2024-04-05",
                title: "劇情電影 B",
                voteAverage: 7.3,
                isCollect: false
            ),
            movie(
                id: 303,
                posterPath: "/poster9.jpg",
                releaseDate: "2022-12-25",
                title: "劇情電影 C",
                voteAverage: 8.4,
                isCollect: true
            )
        ]
    ]

    /// `MovieCardResult` 沒有透過 SKIE 匯出預設參數值，Swift 端建構子每個欄位都要明確帶值，
    /// 這裡包一層 helper 避免假資料每筆都要重複寫滿 16 個參數。
    private static func movie(
        id: Int,
        posterPath: String,
        releaseDate: String,
        title: String,
        voteAverage: Double,
        isCollect: Bool
    ) -> MovieCardResult {
        MovieCardResult(
            adult: false,
            backdropPath: "",
            genreIds: [],
            id: Int32(id),
            originalLanguage: "",
            originalTitle: title,
            overview: "",
            popularity: 0,
            posterPath: posterPath,
            releaseDate: releaseDate,
            title: title,
            video: false,
            voteAverage: voteAverage,
            voteCount: 0,
            isCollect: isCollect,
            timestamp: 0
        )
    }
}
