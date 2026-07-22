import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
        InitKoinIosKt.doInitKoinIos(isDebug: false)

        let theme = ThemeMode.dark
        print("ThemeMode from Shared: \(theme)")
    }

    var body: some Scene {
        WindowGroup {
            MainView()
        }
    }
}



