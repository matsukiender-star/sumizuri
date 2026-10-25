package com.sumizuri.app.data.db

import androidx.room3.Room
import androidx.room3.RoomDatabase

fun databaseBuilder(file: String): RoomDatabase.Builder<AppDatabase> = Room.databaseBuilder<AppDatabase>(name = file)
