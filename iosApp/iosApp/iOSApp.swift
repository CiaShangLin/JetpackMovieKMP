import Shared
import SwiftUI

@main
struct IosApp: App {
    @State
    private var isSplashFinished = false

    @State
    private var themeMode: ThemeMode = .system

    init() {
        InitKoinIosKt.doInitKoinIos(isDebug: AppDebugFlag.isDebugLoggingEnabled)
    }

    var body: some Scene {
        WindowGroup {
            Group {
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
            .preferredColorScheme(themeMode.toColorScheme())
            .task {
                for await userData in KoinHelper.shared.userDataRepository().userData {
                    themeMode = userData.themeMode
                }
            }
        }
   
    }
}
