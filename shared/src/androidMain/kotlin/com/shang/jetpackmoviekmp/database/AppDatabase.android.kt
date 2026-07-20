package com.shang.jetpackmoviekmp.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

/**
 * 建立以 Android app-owned database 目錄為後盾的 [AppDatabase] builder。
 *
 * 資料庫檔案位於 [Context.getDatabasePath]，呼叫端不需自行處理檔案路徑。
 *
 * @param context Android context，用以取得 app-owned database 目錄。
 * @return 尚未 build 的 [RoomDatabase.Builder]，交由 [getRoomDatabase] 完成設定與建立。
 */
fun getDatabaseBuilder(context: Context): RoomDatabase.Builder<AppDatabase> {
    val dbFile = context.getDatabasePath(AppDatabase.DB_NAME)
    return Room.databaseBuilder<AppDatabase>(
        context = context.applicationContext,
        name = dbFile.absolutePath,
    )
}
