package com.shang.jetpackmoviekmp.database

import androidx.room.RoomDatabase

/**
 * Creates a platform-specific in-memory [AppDatabase] builder for domain module tests.
 */
expect fun getTestDatabaseBuilder(): RoomDatabase.Builder<AppDatabase>
