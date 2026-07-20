package com.shang.jetpackmoviekmp.database

import android.app.Application
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Creates an in-memory [AppDatabase] builder for Android host tests.
 */
actual fun getTestDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> =
    Room.inMemoryDatabaseBuilder<AppDatabase>(context = Application())
        .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
