import Observation
import Shared

@MainActor
@Observable
final class FavoritesViewModel {
    private let movieRepository: MovieRepository
    private let toggler: MovieCollectToggler

    private(set) var uiState: FavoritesUiState = .empty

    init(movieRepository: MovieRepository, toggler: MovieCollectToggler = MovieCollectToggler()) {
        self.movieRepository = movieRepository
        self.toggler = toggler
    }

    func loadFavorites() async {
        for await movies in movieRepository.getAllMovieCollect() {
            guard !Task.isCancelled else { return }

            uiState = FavoritesUiState.make(movies: movies)
        }
    }

    /// 切換單一收藏清單卡片的收藏狀態，實際寫入委派給 `toggler`。
    /// - Parameter data: 使用者點擊收藏按鈕的電影卡片，`movieCardIsCollect` 決定要新增還是移除。
    func toggleMovieCollectStatus(data: MovieCardData) async {
        await toggler.toggle(currentIsCollect: data.movieCardIsCollect, movie: data.asMovieCardResult())
    }
}
