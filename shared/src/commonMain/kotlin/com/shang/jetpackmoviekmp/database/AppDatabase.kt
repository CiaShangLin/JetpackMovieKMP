package com.shang.jetpackmoviekmp.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.shang.jetpackmoviekmp.database.dao.MovieCollectDao
import com.shang.jetpackmoviekmp.database.dao.MovieHistoryDao
import com.shang.jetpackmoviekmp.database.entity.MovieCollectEntity
import com.shang.jetpackmoviekmp.database.entity.MovieHistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

/**
 * shared 本地電影資料庫，持久化電影收藏（[MovieCollectEntity]）與瀏覽紀錄（[MovieHistoryEntity]）。
 */
@Database(
    entities = [
        MovieCollectEntity::class,
        MovieHistoryEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {

    companion object {
        const val DB_NAME: String = "JetpackMovieKmpDatabase"
    }

    abstract fun createMovieCollectDao(): MovieCollectDao
    abstract fun createMovieHistoryDao(): MovieHistoryDao
}

/**
 * Room KMP 要求 `@Database` 類別搭配一個 `expect object`，由 room-compiler 在
 * Android／iosArm64／iosSimulatorArm64 三個 target 各自產生對應的 `actual`
 * 實作（呼叫 Room 生成的 `AppDatabase_Impl`），不需手動撰寫平台實作。
 */
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

/**
 * 統一設定 [BundledSQLiteDriver] 與 query coroutine context 後建立 [AppDatabase]。
 * 平台差異（資料庫檔案路徑取得方式）留在呼叫端建立 [builder] 時處理。
 */
fun getRoomDatabase(builder: RoomDatabase.Builder<AppDatabase>): AppDatabase =
    builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
