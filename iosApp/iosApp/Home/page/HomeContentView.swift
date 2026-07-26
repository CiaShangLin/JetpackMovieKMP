import Shared
import SwiftUI

struct HomeContentView: View {
    let movieGenre: MovieGenreBean.MovieGenre

    @State
    private var viewModel: HomeContentViewModel

    init(movieGenre: MovieGenreBean.MovieGenre) {
        self.movieGenre = movieGenre
        self.viewModel = HomeContentViewModel(
            movieGenre: movieGenre,
            movieRepository: KoinHelper.shared.getMovieRepository(),
            homeMovieListPresenter: KoinHelper.shared
                .createHomeMovieListPresenter(
                    withGenres:String(movieGenre.id)
                )
        )
    }
    var body: some View {
        Text("\(movieGenre)")
    }
}
