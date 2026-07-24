/// AppLogger 的全域可替換注入點
enum AppLog {
    static var logger: AppLogger = OSAppLogger(isDebug: AppDebugFlag.isDebugLoggingEnabled)
}
