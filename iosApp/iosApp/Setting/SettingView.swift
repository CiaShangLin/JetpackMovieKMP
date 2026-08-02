import Shared
import SwiftUI

struct SettingView: View {
    @State
    private var viewModel: SettingViewModel

    @State
    private var showThemeDialog = false
    @State
    private var showLanguageDialog = false
    @State
    private var showDeveloperInfo = false

    init() {
        _viewModel = State(
            initialValue: SettingViewModel(userDataRepository: KoinHelper.shared.userDataRepository())
        )
    }

    var body: some View {
        List {
            Button {
                showThemeDialog = true
            } label: {
                settingRow(titleKey: "setting_theme_title", valueKey: themeValueKey)
            }.buttonStyle(.plain)

            Button {
                showLanguageDialog = true
            } label: {
                settingRow(titleKey: "setting_language_title", valueKey: languageValueKey)
            }.buttonStyle(.plain)

            Button {
                showDeveloperInfo = true
            } label: {
                settingRow(titleKey: "setting_developer_title", valueKey: nil)
            }.buttonStyle(.plain)
        }.task {
            await viewModel.observeUserData()
        }.confirmationDialog(
            "setting_theme_title",
            isPresented:
            $showThemeDialog,
            titleVisibility: .visible
        ) {
            Button("setting_theme_light") { Task { await
                    viewModel.setThemeMode(.light)
            } }
            Button("setting_theme_dark") { Task { await
                    viewModel.setThemeMode(.dark)
            } }
            Button("setting_theme_system") { Task { await
                    viewModel.setThemeMode(.system)
            } }
            Button("common_cancel", role: .cancel) {}
        }
        .confirmationDialog(
            "setting_language_title",
            isPresented:
            $showLanguageDialog,
            titleVisibility: .visible
        ) {
            Button("setting_language_system_default") { Task { await
                    viewModel.setLanguageMode(.systemDefault)
            } }
            Button("setting_language_traditional_chinese") { Task { await
                    viewModel.setLanguageMode(.traditionalChinese)
            } }
            Button("setting_language_english") { Task { await
                    viewModel.setLanguageMode(.english)
            } }
            Button("common_cancel", role: .cancel) {}
        }
        .sheet(isPresented: $showDeveloperInfo) {
            VStack(alignment: .leading, spacing: JMSpacing.spacing12) {
                Text("JetpackMovieKMP")
                    .font(.title2)

                Text("setting_developer_name_label")
                    .font(.headline)
                Text("蔡尚霖")

                Text("setting_developer_tech_stack_label")
                    .font(.headline)
                Text("Kotlin Multiplatform")

                Text("setting_developer_github_label")
                    .font(.headline)
                Link("https://github.com/CiaShangLin", destination: URL(string:
                    "https://github.com/CiaShangLin")!)
            }
            .padding(JMSpacing.spacing24)
            .presentationDetents([.medium])
        }
    }

    private func settingRow(titleKey: LocalizedStringKey, valueKey: LocalizedStringKey?) -> some View {
        HStack {
            Text(titleKey)
                .foregroundStyle(.primary)
            Spacer()
            if let valueKey {
                Text(valueKey)
                    .foregroundStyle(.secondary)
            }
            Image(systemName: "chevron.right")
                .foregroundStyle(.tertiary)
        }
    }

    private var themeValueKey: LocalizedStringKey {
        switch viewModel.userData.themeMode {
        case .light: "setting_theme_light"
        case .dark: "setting_theme_dark"
        case .system: "setting_theme_system"
        }
    }

    private var languageValueKey: LocalizedStringKey {
        switch viewModel.userData.languageMode {
        case .systemDefault: "setting_language_system_default"
        case .traditionalChinese: "setting_language_traditional_chinese"
        case .english: "setting_language_english"
        }
    }
}
