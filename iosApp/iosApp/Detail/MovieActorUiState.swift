//
// Created by 蔡尚霖 on 2026/8/3.
//

import Foundation
import Shared

enum MovieActorUiState {
    case loading
    case success(MovieCastAndCrewBean)
    case failure(String)
}
