/// 分頁清單「載入更多」Footer 的顯示狀態，與 shared 層 Kotlin 型別無關；
/// 供各畫面將自身的 append LoadState（例如 `HomeMovieListLoadState`、
/// `SearchMovieListLoadState`）映射後交給 `PagingAppendFooterView` 使用。
enum AppendLoadState {
    /// 目前沒有進行中的載入更多請求，Footer 不顯示任何內容。
    case idle
    /// 正在載入下一頁，Footer 顯示行內載入指示。
    case loading
    /// 載入下一頁失敗，Footer 顯示重試按鈕；`message` 為對應的錯誤說明文字。
    case error(message: String)
}
