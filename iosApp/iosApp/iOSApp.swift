import Shared
import SwiftUI

@main
struct IosApp: App {
    @State
    private var isSplashFinished = false

    init() {
        InitKoinIosKt.doInitKoinIos(isDebug: AppDebugFlag.isDebugLoggingEnabled)
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
