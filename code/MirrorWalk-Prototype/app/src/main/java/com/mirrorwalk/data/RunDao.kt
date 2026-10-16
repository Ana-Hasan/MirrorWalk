package com.mirrorwalk.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RunDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(run: RunEntity): Long

    @Query("SELECT * FROM runs WHERE id = :id")
    fun observeById(id: Long): Flow<RunEntity?>

    @Query("SELECT * FROM runs ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<RunEntity>>

    @Query("DELETE FROM runs WHERE id = :id")
    suspend fun deleteById(id: Long)
}
