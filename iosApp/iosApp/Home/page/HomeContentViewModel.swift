import Observation
import Shared

@Observable
@MainActor
final class HomeContentViewModel {

    private let movieGenre: MovieGenreBean.MovieGenre
    private let movieRepository: MovieRepository
    private let homeMovieListPresenter: HomeMovieListPresenter
    private(set) var state: HomeContentUiState = .loading
    private(set) var itemCount: Int = 0
    private(set) var appendLoadState: HomeMovieListLoadState?

    init(
        movieGenre: MovieGenreBean.MovieGenre,
        movieRepository: MovieRepository,
        homeMovieListPresenter: HomeMovieListPresenter
    ) {
        self.movieGenre = movieGenre
        self.movieRepository = movieRepository
        self.homeMovieListPresenter = homeMovieListPresenter
    }

    deinit {
        homeMovieListPresenter.clear()
    }

    func start() async {
        async let pages: Void = observePagesUpdated()
        async let loadStates: Void = observeLoadStates()
        _ = await (pages, loadStates)
    }

    func item(at index: Int) -> MovieCardResult? {
        homeMovieListPresenter.get(index: Int32(index))
    }

    func refresh() {
        homeMovieListPresenter.refresh()
    }

    func retry() {
        homeMovieListPresenter.retry()
    }

    private func observePagesUpdated() async {
        for await _ in homeMovieListPresenter.onPagesUpdatedFlow {
            itemCount = homeMovieListPresenter.snapshot().count
            if itemCount > 0 {
                state = .success
            }
        }
    }

    private func observeLoadStates() async {
        for await combined in homeMovieListPresenter.loadStateFlow {
            appendLoadState = combined.append

            guard itemCount == 0 else { continue }
            switch onEnum(of: combined.refresh) {
            case .loading:
                state = .loading
            case .idle:
                break
            case .error(let error):
                state = .failure(message: error.message)
            }
        }
    }
}
