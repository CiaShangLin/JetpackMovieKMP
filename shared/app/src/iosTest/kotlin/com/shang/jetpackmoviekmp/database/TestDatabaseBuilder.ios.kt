package com.shang.jetpackmoviekmp.database

import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Creates an in-memory [AppDatabase] builder for iOS tests.
 */
actual fun getTestDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> =
    Room.inMemoryDatabaseBuilder<AppDatabase>()
