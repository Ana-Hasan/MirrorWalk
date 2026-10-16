package com.mirrorwalk.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "runs")
data class RunEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long,
    val endedAt: Long,
    val totalDistanceMeters: Double,
    val averagePaceSecondsPerKm: Double,
    /** JSON array retaining every latitude, longitude, and captured timestamp. */
    val pathJson: String
)

data class RecordedPoint(val latitude: Double, val longitude: Double, val timestamp: Long)
