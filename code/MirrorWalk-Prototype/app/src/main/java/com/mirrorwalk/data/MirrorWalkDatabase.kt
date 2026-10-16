package com.mirrorwalk.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [RunEntity::class], version = 1, exportSchema = false)
abstract class MirrorWalkDatabase : RoomDatabase() {
    abstract fun runDao(): RunDao
}
