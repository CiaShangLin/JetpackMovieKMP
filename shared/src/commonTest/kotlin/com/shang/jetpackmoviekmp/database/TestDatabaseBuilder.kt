package com.shang.jetpackmoviekmp.database

import androidx.room.RoomDatabase

/**
 * 測試專用的 [AppDatabase] builder，每次呼叫回傳指向獨立暫存檔案的 builder，
 * 避免測試之間互相污染資料。平台差異（暫存檔案路徑取得方式）留在各平台實作。
 */
expect fun getTestDatabaseBuilder(): RoomDatabase.Builder<AppDatabase>
