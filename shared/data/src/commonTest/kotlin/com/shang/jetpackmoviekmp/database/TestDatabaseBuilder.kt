package com.shang.jetpackmoviekmp.database

import androidx.room.RoomDatabase

/**
 * Creates a platform-specific in-memory [AppDatabase] builder for data module tests.
 */
expect fun getTestDatabaseBuilder(): RoomDatabase.Builder<AppDatabase>
