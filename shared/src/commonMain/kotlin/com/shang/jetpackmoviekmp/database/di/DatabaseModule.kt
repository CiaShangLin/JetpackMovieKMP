package com.shang.jetpackmoviekmp.database.di

import androidx.room.RoomDatabase
import com.shang.jetpackmoviekmp.database.AppDatabase
import com.shang.jetpackmoviekmp.database.getRoomDatabase
import org.koin.dsl.module

/**
 * 提供本地電影資料庫相關依賴：[AppDatabase] 與其 DAO。
 *
 * @param databaseBuilder 建立平台 [RoomDatabase.Builder] 的 lambda（見 `getDatabaseBuilder`）；只在
 *   `AppDatabase` 第一次被 resolve 時才會呼叫，避免提早解析資料庫檔案路徑。
 */
fun databaseModule(databaseBuilder: () -> RoomDatabase.Builder<AppDatabase>) = module {
    single { getRoomDatabase(databaseBuilder()) }
    single { get<AppDatabase>().createMovieCollectDao() }
    single { get<AppDatabase>().createMovieHistoryDao() }
}
