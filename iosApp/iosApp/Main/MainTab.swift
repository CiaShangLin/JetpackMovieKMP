import SwiftUI

/// iOS 主畫面的底部導覽項目。
enum MainTab: CaseIterable, Hashable {
    case home
    case favorite
    case search
    case history
    case setting

    /// 底部導覽列顯示的在地化標題 key。
    var titleKey: LocalizedStringKey {
        switch self {
        case .home:
            "nav_home"
        case .favorite:
            "nav_favorite"
        case .search:
            "nav_search"
        case .history:
            "nav_history"
        case .setting:
            "nav_setting"
        }
    }

    /// 對應 SF Symbols 的系統圖示名稱。
    var iconName: String {
        switch self {
        case .home:
            "house"
        case .favorite:
            "heart"
        case .search:
            "magnifyingglass"
        case .history:
            "clock"
        case .setting:
            "gearshape"
        }
    }

    /// 目前 tab 對應的內容頁面。
    @ViewBuilder
    var content: some View {
        switch self {
        case .home:
            HomeView()
        case .favorite:
            FavoritesView()
        case .search:
            SearchView()
        case .history:
            HistoryView()
        case .setting:
            SettingView()
        }
    }
}
