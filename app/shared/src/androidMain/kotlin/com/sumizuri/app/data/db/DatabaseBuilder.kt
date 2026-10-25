package com.sumizuri.app.data.db

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase

// The file the first profile uses, which is the one the app had before profiles.
fun legacyDatabaseFile(context: Context): String =
    context.applicationContext.getDatabasePath("sumizuri.db").absolutePath

fun databaseBuilder(context: Context, file: String): RoomDatabase.Builder<AppDatabase> =
    Room.databaseBuilder<AppDatabase>(context = context.applicationContext, name = file)
