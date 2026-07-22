import SwiftUI
import Shared

struct MainView: View {

    @State private var mainViewModel = MainViewModel()
    let bean:MovieGenreBean = MovieGenreBean(
        genres: [
            MovieGenreBean.MovieGenre(id: 1, name: "Gen"),
        ]
    )
 
    var body: some View {
        Text("Hello, World!：\(bean)")
    }
}
