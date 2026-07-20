package com.shang.jetpackmoviekmp.database

import android.app.Application
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Android host test 環境沒有 instrumentation 提供的真實 [android.content.Context]，
 * 呼叫 `context.getDatabasePath(...)` 之類的方法會直接丟出例外（"not mocked"）。
 * 用 [Room.inMemoryDatabaseBuilder] 可以完全避開檔案路徑解析——Room 只在資料庫檔名不是
 * `:memory:` 時才會呼叫 `context.getDatabasePath`，in-memory 模式下 context 只被當成
 * 型別佔位使用，因此未附加 base context 的 [Application] 佔位即可。
 */
actual fun getTestDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> =
    Room.inMemoryDatabaseBuilder<AppDatabase>(context = Application())
        // Host test 用的 Application 沒有附加真正的 base context，AUTOMATIC 模式會呼叫
        // context.getSystemService() 判斷裝置 RAM 進而丟出例外，這裡固定用 TRUNCATE 避開。
        .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
