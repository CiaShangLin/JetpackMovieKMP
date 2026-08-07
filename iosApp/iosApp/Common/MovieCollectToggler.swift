import Shared

/// 收藏切換依賴的抽象介面；只取 `MovieRepository` 用到的兩支方法，
/// 讓測試不需要為完整的 `MovieRepository` 建立 Fake。
///
/// `MovieRepository`本身是 Kotlin interface 匯出的 Swift protocol，protocol 不能用
/// extension 補上新的繼承關係，因此改用一個轉發用的 adapter 包住真正的 repository。
protocol MovieCollectionToggling {
    /// 新增一筆電影收藏。
    /// - Parameter movieResult: 欲加入收藏的電影。
    func insertMovieCollect(movieResult: MovieCardResult) async throws

    /// 移除一筆電影收藏。
    /// - Parameter movieResult: 欲移除收藏的電影。
    func deleteMovieCollect(movieResult: MovieCardResult) async throws
}

/// 將具體的 `MovieRepository` 轉接為 `MovieCollectionToggling`，供 `MovieCollectToggler` 使用。
struct MovieRepositoryCollectionAdapter: MovieCollectionToggling {
    let repository: MovieRepository

    func insertMovieCollect(movieResult: MovieCardResult) async throws {
        try await repository.insertMovieCollect(movieResult: movieResult)
    }

    func deleteMovieCollect(movieResult: MovieCardResult) async throws {
        try await repository.deleteMovieCollect(movieResult: movieResult)
    }
}

/// 收斂「防連點 guard + insert-or-delete 判斷 + 呼叫 repository + 錯誤處理」這段
/// 原本在多個 ViewModel 各自重複的收藏切換邏輯。每個 ViewModel 應各自持有獨立的
/// `MovieCollectToggler` 實例，避免不相關的收藏操作互相阻擋。
@MainActor
final class MovieCollectToggler {
    private let repository: MovieCollectionToggling
    private var isUpdatingCollection = false

    /// 建立收藏切換器。
    /// - Parameter repository: 實際執行收藏寫入的依賴，預設透過 Koin 解析目前的 `MovieRepository`。
    ///
    /// 標記 `nonisolated`：init 只做單純賦值，不觸碰任何 actor-isolated 狀態；
    /// 呼叫端（各 ViewModel）的 init 常以 `MovieCollectToggler()` 作為參數預設值，
    /// 而 Swift 的預設值運算式一律在非隔離上下文求值，若此 init 維持隱含的
    /// `@MainActor` 隔離會導致那些呼叫端無法編譯。
    nonisolated init(repository: MovieCollectionToggling = MovieRepositoryCollectionAdapter(
        repository: KoinHelper.shared.getMovieRepository()
    )) {
        self.repository = repository
    }

    /// 依目前收藏狀態切換收藏（已收藏則移除、未收藏則加入），並防止同一實例上的連續呼叫互相搶跑。
    /// - Parameters:
    ///   - currentIsCollect: 呼叫當下該電影是否已收藏，決定要新增還是移除。
    ///   - movie: 欲切換收藏狀態的電影。
    func toggle(currentIsCollect: Bool, movie: MovieCardResult) async {
        guard !isUpdatingCollection else { return }

        isUpdatingCollection = true
        defer { isUpdatingCollection = false }

        do {
            if currentIsCollect {
                try await repository.deleteMovieCollect(movieResult: movie)
            } else {
                try await repository.insertMovieCollect(movieResult: movie)
            }
        } catch {
            print("切換收藏失敗：\(error.localizedDescription)")
        }
    }
}
