import Shared

/// 測試用的最小 `CoroutineDispatcher` 實作，供建構需要 `ioDispatcher` 參數的 UseCase
/// （例如 `GetHistoryMovieListUseCase`）；測試不會實際觸發任何協程排程，`dispatch` 不需要真的實作。
final class FakeCoroutineDispatcher: Kotlinx_coroutines_coreCoroutineDispatcher {
    override func dispatch(context: any KotlinCoroutineContext, block: any Kotlinx_coroutines_coreRunnable) {
        fatalError("not used in these tests")
    }
}
