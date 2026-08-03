import Shared
import SwiftUI

/// 搜尋分頁的暫時內容頁。
struct SearchView: View {
    @State
    private var searchText = ""
    @State
    private var viewModel = SearchViewModel()
    @State
    private var path = NavigationPath()

    var body: some View {
        NavigationStack(path: $path) {
            content
                .navigationTitle("search_title")
                .navigationDestination(for: Int.self) { movieId in
                    MovieDetailView(movieId: movieId, path: $path)
                }
        }.searchable(text: $searchText, prompt: Text("search_field_prompt"))
            .onSubmit(of: .search) {
                viewModel.submit(query: searchText)
            }
            .task {
                await viewModel.observeLanguageMode()
            }
    }

    @ViewBuilder
    private var content: some View {
        switch viewModel.state {
        case .initial:
            Text("search_initial_guide")
                .foregroundStyle(.secondary)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        case .loading:
            LoadingView()
        case .results:
            if viewModel.movies.isEmpty {
                Text("search_no_results")
                    .foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                ScrollView {
                    LazyVGrid(
                        columns: [
                            GridItem(.adaptive(minimum: JMSize.movieGridMinWidth), spacing: JMSpacing.spacing12)
                        ], spacing: JMSpacing.spacing12
                    ) {
                        ForEach(Array(viewModel.movies.enumerated()), id: \.element.id) { index, movie in
                            MovieCardView(
                                data: movie.asMovieCardData(),
                                onMovieTap: { movie in
                                    path.append(Int(movie.movieCardId))
                                },
                                onCollectTap: { movie in
                                    Task {
                                        await viewModel.toggleMovieCollectStatus(data: movie)
                                    }
                                }
                            )
                            .onAppear {
                                viewModel.prefetch(index: index)
                            }
                        }
                    }
                }.padding(JMSpacing.spacing8)
                    .refreshable {
                        viewModel.refresh()
                    }
                appendFooter
            }
        case let .failure(message):
            ErrorView(
                message: LocalizedStringKey(message),
                onRetry: { viewModel.retry() }
            )
        }
    }

    @ViewBuilder
    private var appendFooter: some View {
        if let appendLoadState = viewModel.appendLoadState {
            switch onEnum(of: appendLoadState) {
            case .idle:
                EmptyView()
            case .loading:
                ProgressView().padding()
            case .error:
                Button("home_retry_button") {
                    viewModel.retry()
                }
                .padding()
            }
        }
    }
}
