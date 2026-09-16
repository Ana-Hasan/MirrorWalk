package com.mirrorwalk.data

import org.json.JSONArray
import org.json.JSONObject

class RunRepository(private val dao: RunDao) {
    suspend fun save(
        startedAt: Long,
        endedAt: Long,
        distanceMeters: Double,
        averagePace: Double,
        points: List<RecordedPoint>
    ): Long = dao.insert(RunEntity(
        startedAt = startedAt,
        endedAt = endedAt,
        totalDistanceMeters = distanceMeters,
        averagePaceSecondsPerKm = averagePace,
        pathJson = encode(points)
    ))

    fun observeRun(id: Long) = dao.observeById(id)
    fun observeRuns() = dao.observeAll()
    suspend fun deleteRun(id: Long) = dao.deleteById(id)

    fun decodePath(json: String): List<RecordedPoint> = JSONArray(json).let { array ->
        List(array.length()) { index ->
            array.getJSONObject(index).let { point ->
                RecordedPoint(point.getDouble("lat"), point.getDouble("lng"), point.getLong("time"))
            }
        }
    }

    private fun encode(points: List<RecordedPoint>): String = JSONArray().also { array ->
        points.forEach { point ->
            array.put(JSONObject().apply {
                put("lat", point.latitude)
                put("lng", point.longitude)
                put("time", point.timestamp)
            })
        }
    }.toString()
}
