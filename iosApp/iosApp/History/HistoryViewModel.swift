import Observation
import Shared

@Observable
@MainActor
final class HistoryViewModel {
    private let movieRepository: MovieRepository
    private let getHistoryMovieListUseCase: GetHistoryMovieListUseCase
    private let toggler: MovieCollectToggler
    private(set) var uiState: HistoryUiState = .empty
    private var isClearingHistory = false
    init(
        movieRepository: MovieRepository,
        getHistoryMovieListUseCase: GetHistoryMovieListUseCase,
        toggler: MovieCollectToggler = MovieCollectToggler()
    ) {
        self.movieRepository = movieRepository
        self.getHistoryMovieListUseCase = getHistoryMovieListUseCase
        self.toggler = toggler
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

    /// 切換單一歷史紀錄卡片的收藏狀態，實際寫入委派給 `toggler`。
    /// - Parameter data: 使用者點擊收藏按鈕的電影卡片，`movieCardIsCollect` 決定要新增還是移除。
    func toggleMovieCollectStatus(data: MovieCardData) async {
        await toggler.toggle(currentIsCollect: data.movieCardIsCollect, movie: data.asMovieCardResult())
    }
}
