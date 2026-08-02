import Shared
import SwiftUI

extension ThemeMode {
    func toColorScheme() -> ColorScheme? {
        switch self {
        case .light:
            .light
        case .dark:
            .dark
        case .system:
            nil
        }
    }
}
