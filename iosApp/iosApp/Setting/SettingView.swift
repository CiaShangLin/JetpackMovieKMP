import Shared
import SwiftUI

struct SettingView: View {
    @State
    private var viewModel: SettingViewModel

    init() {
        _viewModel = State(
            initialValue: SettingViewModel(userDataRepository: KoinHelper.shared.userDataRepository())
        )
    }

    var body: some View {
        Text("main_setting_placeholder")
            .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
