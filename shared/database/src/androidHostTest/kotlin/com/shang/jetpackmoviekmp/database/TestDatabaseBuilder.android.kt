package com.shang.jetpackmoviekmp.database

import android.app.Application
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Creates an in-memory [AppDatabase] builder for Android host tests.
 */
actual fun getTestDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> =
    Room.inMemoryDatabaseBuilder<AppDatabase>(context = Application())
        // Android host tests do not provide a fully initialized base context.
        .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
