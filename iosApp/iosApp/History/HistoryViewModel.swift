import Observation
import Shared

@Observable
@MainActor
final class HistoryViewModel {
    private let movieRepository: MovieRepository
    private let getHistoryMovieListUseCase: GetHistoryMovieListUseCase
    private(set) var uiState: HistoryUiState = .empty
    private var isUpdatingCollection = false
    private var isClearingHistory = false
    init(
        movieRepository: MovieRepository,
        getHistoryMovieListUseCase: GetHistoryMovieListUseCase
    ) {
        self.movieRepository = movieRepository
        self.getHistoryMovieListUseCase = getHistoryMovieListUseCase
    }

    func loadHistory() async {
        for await movies in getHistoryMovieListUseCase.invoke() {
            guard !Task.isCancelled else { return }

            uiState = HistoryUiState.make(movies: movies)
        }
    }

    func clearHistory() async {
        guard !isClearingHistory else { return }

        isClearingHistory = true
        defer { isClearingHistory = false }

        do {
            _ = try await movieRepository.deleteAllMovieHistory()
        } catch {
            print("清空歷史失敗：\(error.localizedDescription)")
        }
    }

    func toggleMovieCollect(data: MovieCardData) async {
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
