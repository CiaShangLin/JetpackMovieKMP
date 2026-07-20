package com.shang.jetpackmoviekmp.database

import androidx.room.Room
import androidx.room.RoomDatabase
import com.shang.jetpackmoviekmp.common.resolveIosDocumentDirectoryPath

/**
 * 建立以穩定 app document 路徑為後盾的 [AppDatabase] builder，確保 app 重啟後仍可讀取。
 *
 * @return 尚未 build 的 [RoomDatabase.Builder]，交由 [getRoomDatabase] 完成設定與建立。
 * @throws IllegalArgumentException 當無法解析 iOS document directory 時拋出。
 */
fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val dbFilePath = resolveIosDocumentDirectoryPath(AppDatabase.DB_NAME)
    return Room.databaseBuilder<AppDatabase>(name = dbFilePath)
}
