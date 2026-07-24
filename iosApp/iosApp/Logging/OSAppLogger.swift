import Foundation
import os

/// 包裝系統原生 os.Logger 的預設 AppLogger 實作
/// isDebug = false 時只輸出 warning/error，避免正式版洩漏 debug/info 內容
struct OSAppLogger: AppLogger {
    private let isDebug: Bool
    private let subsystem: String

    init(isDebug: Bool, subsystem: String = Bundle.main.bundleIdentifier ?? "JetpackMovieKMP") {
        self.isDebug = isDebug
        self.subsystem = subsystem
    }

    func debug(_ message: @autoclosure () -> String, category: String) {
        // isDebug = false 時提早 return，message() 完全不會被呼叫，避免付出組字串的成本
        guard isDebug else { return }
        let resolvedMessage = message()
        logger(category: category).debug("\(resolvedMessage, privacy: .public)")
    }

    func info(_ message: @autoclosure () -> String, category: String) {
        // 同上：提早 return 讓 @autoclosure 包住的字串插值不會被求值
        guard isDebug else { return }
        let resolvedMessage = message()
        logger(category: category).info("\(resolvedMessage, privacy: .public)")
    }

    func warning(_ message: @autoclosure () -> String, category: String) {
        let resolvedMessage = message()
        logger(category: category).warning("\(resolvedMessage, privacy: .public)")
    }

    func error(_ message: @autoclosure () -> String, category: String) {
        let resolvedMessage = message()
        logger(category: category).error("\(resolvedMessage, privacy: .public)")
    }

    private func logger(category: String) -> os.Logger {
        os.Logger(subsystem: subsystem, category: category)
    }
}
