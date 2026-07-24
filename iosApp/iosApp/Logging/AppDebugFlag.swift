import Foundation

/// 判斷目前執行環境是否視為 debug，供 Koin 初始化與 AppLogger 共用同一份判斷來源
enum AppDebugFlag {
    static var isDebugLoggingEnabled: Bool {
        let value = ProcessInfo.processInfo.environment["JM_DEBUG_NETWORK"]?.lowercased()

        if ["1", "true", "yes"].contains(value) {
            return true
        }

        if ["0", "false", "no"].contains(value) {
            return false
        }

        #if DEBUG
            return true
        #else
            return false
        #endif
    }
}
