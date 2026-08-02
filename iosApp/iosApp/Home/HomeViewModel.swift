import Observation
import Shared

@Observable
@MainActor
final class HomeViewModel {
    private let movieRepository: MovieRepository
    private(set) var uiState: HomeUiState = .loading
    /// `deinit` 在 Swift 是 nonisolated context，無法直接存取 MainActor-isolated 的 var，
    /// 用 nonisolated(unsafe) 讓 deinit 能同步呼叫 clear()；deinit 執行時保證沒有其他程式碼
    /// 會同時存取這個物件，因此手動略過隔離檢查是安全的。
    private nonisolated(unsafe) var presenters: [Int32: HomeMovieListPresenter] = [:]
    private var lastLanguageMode: LanguageMode?

    init(movieRepository: MovieRepository) {
        self.movieRepository = movieRepository
    }

    deinit {
        presenters.values.forEach { $0.clear() }
    }

    func loadHome() async {
        uiState = HomeUiState.loading
        for await result in movieRepository.getMovieGenres() {
            switch onEnum(of: result) {
            case let .success(success):
                guard let data = success.data as? MovieGenreBean else {
                    uiState = .failure(debugMessage: "電影分類資料格式錯誤")
                    return
                }
                let genres = data.genres
                uiState = .success(genres: genres)
            case let .failure(failure):
                switch onEnum(of: failure.error) {
                case let .network(network):
                    uiState = .failure(debugMessage: network.exception.message ?? "網路錯誤，請稍後再試")
                case .unknown:
                    uiState = .failure(debugMessage: "發生未知錯誤")
                }
            }
            return
        }
    }

    func retry() async {
        await loadHome()
    }

    /// 監聽語言模式變化，變化時 refresh 已建立的 presenter，讓已載入的清單改用新語言重新請求。
    /// 只在「與上一次觀察到的值不同」時觸發，避免第一次收到 userData 就誤判成一次變化。
    func observeLanguageMode() async {
        for await userData in KoinHelper.shared.userDataRepository().userData {
            let mode = userData.languageMode
            if let last = lastLanguageMode, last != mode {
                presenters.values.forEach { $0.refresh() }
            }
            lastLanguageMode = mode
        }
    }

    /// 延後到對應 genre 分頁真正出現（`HomeContentView.task`）時才第一次呼叫、建立 presenter，
    /// 之後每次呼叫都回傳同一個快取實例，不會重新建立——對應 Android `HorizontalPager` 只組合
    /// 目前可見分頁的 lazy 行為，避免一進首頁就把所有 genre 的 API 同時打出去。
    func presenter(for genre: MovieGenreBean.MovieGenre) -> HomeMovieListPresenter {
        if let existing = presenters[genre.id] {
            return existing
        }
        let created = KoinHelper.shared.createHomeMovieListPresenter(withGenres: String(genre.id))
        presenters[genre.id] = created
        return created
    }
}
