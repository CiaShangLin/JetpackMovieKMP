/// 可替換注入的統一 App Log 介面
///
/// `message` 使用 `@autoclosure` 讓呼叫端可以像傳一般字串一樣呼叫（不用手動包 `{ }`），
/// 但字串插值實際上不會在呼叫當下求值，而是延遲到實作內部呼叫 `message()` 時才執行。
/// 這讓 `isDebug == false` 時可以在組字串前就 `return`，避免白白付出組字串的成本。
protocol AppLogger {
    func debug(_ category: String, _ message: @autoclosure () -> String)
    func info(_ category: String, _ message: @autoclosure () -> String)
    func warning(_ category: String, _ message: @autoclosure () -> String)
    func error(_ category: String, _ message: @autoclosure () -> String)
}
