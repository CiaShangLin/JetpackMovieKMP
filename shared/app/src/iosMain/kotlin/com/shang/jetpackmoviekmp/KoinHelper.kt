package com.shang.jetpackmoviekmp

import com.shang.jetpackmoviekmp.data.repository.UserDataRepository
import com.shang.jetpackmoviekmp.domain.usecase.GetConfigurationUseCase
import com.shang.jetpackmoviekmp.domain.usecase.GetMovieDetailUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

/**
 * 提供 iOS 端可呼叫的 Koin 依賴橋接物件。
 *
 * `KoinHelper` 僅能在 `doInitKoinIos` 啟動 Koin 之後呼叫，否則 Koin 尚未建立全域
 * context 時會無法解析依賴。日後新增 Swift 端需要的依賴時，請在此物件新增具名
 * accessor，例如 `userDataRepository()`，避免將 reified generic API 直接暴露給 Swift。
 *
 * Swift 消費端應由組裝根取得這些依賴，並以建構子參數往下傳遞給 ViewModel 或其他
 * 物件；需要依賴的物件不得在內部直接呼叫 `KoinHelper`。
 */
object KoinHelper : KoinComponent {

    fun getConfigurationUseCase(): GetConfigurationUseCase = getKoin().get()

    /**
     * 解析使用者偏好設定 repository，作為 iOS 端新增 accessor 的命名範例。
     */
    fun userDataRepository(): UserDataRepository = getKoin().get()

    /**
     * 解析電影詳情 UseCase，供 iOS 端驗證與消費 `Flow<Result<MovieDetailBean>>`。
     */
    fun getMovieDetailUseCase(): GetMovieDetailUseCase = getKoin().get()
}
