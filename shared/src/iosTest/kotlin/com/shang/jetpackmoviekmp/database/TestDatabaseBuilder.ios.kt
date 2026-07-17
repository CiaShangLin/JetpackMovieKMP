package com.shang.jetpackmoviekmp.database

import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * 測試用 in-memory database，每次呼叫都是獨立、互不干擾的資料庫，也不會在裝置上留下暫存檔案。
 */
actual fun getTestDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> =
    Room.inMemoryDatabaseBuilder<AppDatabase>()
