import Observation
import Shared

@Observable
@MainActor
final class HomeContentViewModel {
    private let movieGenre: MovieGenreBean.MovieGenre
    private let homeViewModel: HomeViewModel
    private var homeMovieListPresenter: HomeMovieListPresenter?
    private let movieRepository: MovieRepository
    private var isUpdatingCollection = false
    private(set) var state: HomeContentUiState = .loading
    private(set) var movies: [MovieCardResult] = []
    private(set) var itemCount: Int = 0
    private(set) var appendLoadState: HomeMovieListLoadState?

    init(
        movieRepository: MovieRepository,
        movieGenre: MovieGenreBean.MovieGenre,
        homeViewModel: HomeViewModel
    ) {
        self.movieRepository = movieRepository
        self.movieGenre = movieGenre
        self.homeViewModel = homeViewModel
    }

    func start() async {
        // 延後到這裡（畫面真正出現、`.task` 觸發時）才跟 `HomeViewModel` 要 presenter，
        // 對應 genre 才第一次被建立；避免在 `init` 就建立，導致 TabView(.page) 的 ForEach
        // 一次把所有 genre 的 presenter 都建出來、同時打 API。
        let presenter = homeViewModel.presenter(for: movieGenre)
        homeMovieListPresenter = presenter

        // `onPagesUpdatedFlow` 不會對新訂閱者重播過去的事件；presenter 可能在這個畫面
        // 真正出現、開始訂閱之前就已經載入完成，所以先同步讀一次目前的 snapshot 補上既有資料，
        // 再開始監聽之後的更新，避免錯過事件導致永遠停在 Loading。
        updateMovies(from: presenter)
        if itemCount > 0 {
            state = .success(itemCount: itemCount)
        }

        async let pages: Void = observePagesUpdated(presenter: presenter)
        async let loadStates: Void = observeLoadStates(presenter: presenter)
        _ = await (pages, loadStates)
    }

    func prefetch(index: Int) {
        _ = homeMovieListPresenter?.get(index: Int32(index))
    }

    func refresh() {
        homeMovieListPresenter?.refresh()
    }

    func retry() {
        homeMovieListPresenter?.retry()
    }

    func toggleMovieCollectStatus(data: MovieCardData) async {
        guard !isUpdatingCollection else { return }

        isUpdatingCollection = true
        defer { isUpdatingCollection = false }

        do {
            switch MovieCollectAction(data: data) {
            case let .delete(movie):
                try await movieRepository.deleteMovieCollect(movieResult: movie)
            case let .insert(movie):
                try await movieRepository.insertMovieCollect(movieResult: movie)
            }
        } catch {
            print("切換收藏失敗：\(error.localizedDescription)")
        }
    }

    private func observePagesUpdated(presenter: HomeMovieListPresenter) async {
        for await _ in presenter.onPagesUpdatedFlow {
            updateMovies(from: presenter)

            if itemCount > 0 {
                state = .success(itemCount: itemCount)
            }
        }
    }

    private func updateMovies(from presenter: HomeMovieListPresenter) {
        movies = presenter.snapshot().compactMap { $0 as? MovieCardResult }
        itemCount = movies.count
    }

    private func observeLoadStates(presenter: HomeMovieListPresenter) async {
        for await combined in presenter.loadStateFlow {
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
