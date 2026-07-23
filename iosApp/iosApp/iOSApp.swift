import Shared
import SwiftUI

@main
struct iOSApp: App {
    @State
    private var isSplashFinished = false

    init() {
        InitKoinIosKt.doInitKoinIos(isDebug: Self.networkLoggingEnabled)
    }

    var body: some Scene {
        WindowGroup {
            if isSplashFinished {
                MainView()
            } else {
                SplashView {
                    withAnimation {
                        isSplashFinished = true
                    }
                }
            }
        }
    }
}

private extension iOSApp {

    static var networkLoggingEnabled: Bool {
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
