import SwiftUI

/// Splash 完成後顯示的 iOS 主畫面，負責組合五個底部導覽分頁。
struct MainView: View {
    @State
    private var selectedTab: MainTab = .home

    var body: some View {
        TabView(selection: $selectedTab) {
            ForEach(MainTab.allCases, id: \.self) { tab in
                tab.content
                    .tag(tab)
                    .tabItem {
                        Image(systemName: tab.iconName)
                        Text(tab.titleKey)
                    }
            }
        }
    }
}
