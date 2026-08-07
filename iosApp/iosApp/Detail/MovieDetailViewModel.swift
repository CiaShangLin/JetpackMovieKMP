import Foundation
import Shared

@MainActor
@Observable
final class MovieDetailViewModel {
    private let movieId: Int
    private let movieRepository: MovieRepository
    private let getMovieDetailUseCase: GetMovieDetailUseCase
    private let getMovieRecommendUseCase: GetMovieRecommendUseCase
    private let toggler: MovieCollectToggler

    var uiState: MovieDetailUiState = .loading
    var actorUiState: MovieActorUiState = .loading
    var recommendUiState: MovieRecommendUiState = .loading

    var isCollect = false

    init(
        movieId: Int,
        movieRepository: MovieRepository,
        getMovieDetailUseCase: GetMovieDetailUseCase,
        getMovieRecommendUseCase: GetMovieRecommendUseCase,
        toggler: MovieCollectToggler = MovieCollectToggler()
    ) {
        self.movieId = movieId
        self.movieRepository = movieRepository
        self.getMovieDetailUseCase = getMovieDetailUseCase
        self.getMovieRecommendUseCase = getMovieRecommendUseCase
        self.toggler = toggler
    }

    func observeCollectStatus() async {
        for await result in movieRepository.getMovieCollectEntityById(id: Int32(movieId)) {
            isCollect = result != nil
        }
    }

    func fetchMovieDetail() async {
        for await result in getMovieDetailUseCase.invoke(movieId: Int32(movieId)) {
            switch onEnum(of: result) {
            case let .success(success):
                guard let movieDetail = success.data as? MovieDetailBean else {
                    uiState = .failure("電影詳情資料格式錯誤")
                    return
                }
                uiState = .success(movieDetail)
            case let .failure(failure):
                switch onEnum(of: failure.error) {
                case let .network(network):
                    uiState = .failure(network.exception.message ?? "網路錯誤，請稍後再試")
                case .unknown:
                    uiState = .failure("發生未知錯誤")
                }
            }
            return
        }
    }

    func fetchMovieActor() async {
        for await result in movieRepository.getMovieActor(id: Int32(movieId)) {
            switch onEnum(of: result) {
            case let .success(success):
                guard let movieActor = success.data as? MovieCastAndCrewBean else {
                    actorUiState = .failure("電影演員資料格式錯誤")
                    return
                }
                actorUiState = .success(movieActor)
            case let .failure(failure):
                switch onEnum(of: failure.error) {
                case let .network(network):
                    actorUiState = .failure(network.exception.message ?? "網路錯誤，請稍後再試")
                case .unknown:
                    actorUiState = .failure("發生未知錯誤")
                }
            }
            return
        }
    }

    func fetchMovieRecommend() async {
        for await result in getMovieRecommendUseCase.invoke(movieId: Int32(movieId)) {
            switch onEnum(of: result) {
            case let .success(success):
                recommendUiState = .success(success.data as? [MovieCardResult] ?? [])
            case let .failure(failure):
                switch onEnum(of: failure.error) {
                case let .network(network):
                    recommendUiState = .failure(network.exception.message ?? "網路錯誤，請稍後再試")
                case .unknown:
                    recommendUiState = .failure("發生未知錯誤")
                }
            }
        }
    }

    /// 切換目前這部電影詳情頁本身的收藏狀態，實際寫入委派給 `toggler`。
    /// - Parameter data: 目前詳情頁對應的電影；是否已收藏依 `isCollect`（由 `observeCollectStatus()` 維護）判斷。
    func toggleMovieCollectStatus(data: MovieCardResult) async {
        await toggler.toggle(currentIsCollect: isCollect, movie: data)
    }

    /// 切換推薦清單中單一電影卡片的收藏狀態，實際寫入委派給 `toggler`。
    /// - Parameter data: 使用者點擊收藏按鈕的推薦電影卡片，`movieCardIsCollect` 決定要新增還是移除。
    func toggleRecommendCollectStatus(data: MovieCardData) async {
        await toggler.toggle(currentIsCollect: data.movieCardIsCollect, movie: data.asMovieCardResult())
    }
}
