import Foundation
import Shared

@MainActor
@Observable
final class MovieDetailViewModel {
    private let movieId: Int
    private let movieRepository: MovieRepository
    private let getMovieDetailUseCase: GetMovieDetailUseCase
    private let getMovieRecommendUseCase: GetMovieRecommendUseCase

    var uiState: MovieDetailUiState = .loading
    var actorUiState: MovieActorUiState = .loading
    var recommendUiState: MovieRecommendUiState = .loading

    private var isUpdatingCollection = false
    var isCollect = false

    init(
        movieId: Int,
        movieRepository: MovieRepository,
        getMovieDetailUseCase: GetMovieDetailUseCase,
        getMovieRecommendUseCase: GetMovieRecommendUseCase
    ) {
        self.movieId = movieId
        self.movieRepository = movieRepository
        self.getMovieDetailUseCase = getMovieDetailUseCase
        self.getMovieRecommendUseCase = getMovieRecommendUseCase
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

    func toggleMovieCollectStatus(data: MovieCardResult) async {
        guard !isUpdatingCollection else { return }

        isUpdatingCollection = true
        defer { isUpdatingCollection = false }

        do {
            if isCollect {
                try await movieRepository.deleteMovieCollect(movieResult: data)
            } else {
                try await movieRepository.insertMovieCollect(movieResult: data)
            }
        } catch {
            print("切換收藏失敗：\(error.localizedDescription)")
        }
    }

    func toggleRecommendCollectStatus(data: MovieCardData) async {
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
}
