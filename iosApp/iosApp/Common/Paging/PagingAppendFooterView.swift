import SwiftUI

/// 分頁清單行內的「載入更多」Footer；依 `AppendLoadState` 顯示空白、
/// 行內載入指示，或含重試按鈕的錯誤提示，不遮蔽既有清單項目。
struct PagingAppendFooterView: View {
    /// 目前的載入更多狀態，決定 Footer 要顯示空白、載入指示或錯誤重試按鈕。
    let loadState: AppendLoadState
    /// 使用者點擊重試按鈕時呼叫，交由呼叫端觸發實際的重新載入邏輯。
    let onRetry: () -> Void

    var body: some View {
        switch loadState {
        case .idle:
            EmptyView()
        case .loading:
            ProgressView()
                .padding(JMSpacing.spacing8)
        case .error:
            Button("paging_append_retry_button", action: onRetry)
                .padding(JMSpacing.spacing8)
        }
    }
}
