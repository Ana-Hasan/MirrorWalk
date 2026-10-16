package com.mirrorwalk

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.mirrorwalk.data.MirrorWalkDatabase
import com.mirrorwalk.data.RecordedPoint
import com.mirrorwalk.data.RunRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class RunStatus { IDLE, RUNNING, PAUSED, FINISHED }

data class RunUiState(
    val status: RunStatus = RunStatus.IDLE,
    val points: List<RecordedPoint> = emptyList(),
    val totalDistanceMeters: Double = 0.0,
    val elapsedMillis: Long = 0L,
    val startedAt: Long? = null,
    val segmentStartedAt: Long? = null,
    val ghostModeEnabled: Boolean = true,
    val savedRunId: Long? = null,
    val currentPaceSecondsPerKm: Double = 0.0,
    val error: String? = null
)

class RunViewModel(application: Application) : AndroidViewModel(application) {
    private val fusedClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(application)
    private val repository = RunRepository(
        Room.databaseBuilder(application, MirrorWalkDatabase::class.java, "mirrorwalk.db").build().runDao()
    )
    private val _uiState = MutableStateFlow(RunUiState())
    val uiState: StateFlow<RunUiState> = _uiState.asStateFlow()
    private var ticker: Job? = null

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.locations.forEach(::addLocation)
        }
    }

    fun hasLocationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun startOrResume(context: Context) {
        if (!hasLocationPermission(context)) {
            _uiState.value = _uiState.value.copy(error = "Location permission is required to record a route.")
            return
        }
        val old = _uiState.value
        val freshStart = old.status == RunStatus.IDLE || old.status == RunStatus.FINISHED
        val now = System.currentTimeMillis()
        _uiState.value = if (freshStart) {
            RunUiState(status = RunStatus.RUNNING, startedAt = now, segmentStartedAt = now)
        } else {
            old.copy(status = RunStatus.RUNNING, segmentStartedAt = now, error = null)
        }
        fusedClient.requestLocationUpdates(locationRequest, locationCallback, context.mainLooper)
        startTicker()
    }

    fun pause() {
        if (_uiState.value.status != RunStatus.RUNNING) return
        fusedClient.removeLocationUpdates(locationCallback)
        ticker?.cancel()
        _uiState.value = freezeElapsed(_uiState.value).copy(status = RunStatus.PAUSED, segmentStartedAt = null)
    }

    fun stop() {
        val current = if (_uiState.value.status == RunStatus.RUNNING) freezeElapsed(_uiState.value) else _uiState.value
        if (current.status != RunStatus.RUNNING && current.status != RunStatus.PAUSED) return
        fusedClient.removeLocationUpdates(locationCallback)
        ticker?.cancel()
        val endedAt = System.currentTimeMillis()
        val averagePace = if (current.totalDistanceMeters < 10) 0.0 else
            current.elapsedMillis / 1000.0 / (current.totalDistanceMeters / 1000.0)
        _uiState.value = current.copy(status = RunStatus.FINISHED)
        viewModelScope.launch {
            val id = repository.save(current.startedAt ?: endedAt, endedAt, current.totalDistanceMeters, averagePace, current.points)
            _uiState.value = _uiState.value.copy(savedRunId = id)
        }
    }

    fun clearForNewRun() {
        if (_uiState.value.status != RunStatus.RUNNING) _uiState.value = RunUiState()
    }

    /** Ghost mode is chosen before a session and locked once recording begins. */
    fun setGhostModeEnabled(enabled: Boolean) {
        if (_uiState.value.status == RunStatus.IDLE) {
            _uiState.value = _uiState.value.copy(ghostModeEnabled = enabled)
        }
    }

    fun observeRun(id: Long) = repository.observeRun(id)
    fun observeRuns() = repository.observeRuns()
    fun decodePath(json: String) = repository.decodePath(json)
    fun deleteRun(id: Long) = viewModelScope.launch { repository.deleteRun(id) }

    private fun addLocation(location: Location) {
        if (_uiState.value.status != RunStatus.RUNNING || !location.hasAccuracy() || location.accuracy > MAX_ACCURACY_METERS) return
        val point = RecordedPoint(location.latitude, location.longitude, location.time)
        val state = _uiState.value
        val prior = state.points.lastOrNull()
        val increment = prior?.let { last ->
            val results = FloatArray(1)
            Location.distanceBetween(last.latitude, last.longitude, point.latitude, point.longitude, results)
            val secondsSinceLastPoint = ((point.timestamp - last.timestamp) / 1_000.0).coerceAtLeast(0.0)
            val plausibleDistance = MAX_GROUND_SPEED_MPS * secondsSinceLastPoint.coerceAtMost(10.0) + 15.0
            // Drop GPS jitter, car-speed readings, and teleport-style fixes.
            if (results[0] >= MIN_SEGMENT_METERS && results[0] <= plausibleDistance && (!location.hasSpeed() || location.speed <= MAX_GROUND_SPEED_MPS)) results[0].toDouble() else 0.0
        } ?: 0.0
        val points = if (prior == null || increment > 0.0) state.points + point else state.points
        _uiState.value = state.copy(
            points = points,
            totalDistanceMeters = state.totalDistanceMeters + increment,
            currentPaceSecondsPerKm = rollingPace(points)
        )
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (true) {
                val state = _uiState.value
                if (state.status == RunStatus.RUNNING && state.startedAt != null) {
                    val segmentStart = state.segmentStartedAt
                    if (segmentStart != null) {
                        val now = System.currentTimeMillis()
                        _uiState.value = state.copy(elapsedMillis = state.elapsedMillis + now - segmentStart, segmentStartedAt = now)
                    }
                }
                delay(1_000)
            }
        }
    }

    private val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2_000)
        .setMinUpdateDistanceMeters(2f)
        .setMinUpdateIntervalMillis(1_000)
        .setWaitForAccurateLocation(true)
        .build()

    private fun freezeElapsed(state: RunUiState): RunUiState {
        val segmentStart = state.segmentStartedAt ?: return state
        return state.copy(elapsedMillis = state.elapsedMillis + System.currentTimeMillis() - segmentStart)
    }

    /** A 30 second point-to-point pace is more usable than raw GPS instant speed. */
    private fun rollingPace(points: List<RecordedPoint>): Double {
        if (points.size < 2) return 0.0
        val latest = points.last()
        val windowStart = latest.timestamp - ROLLING_PACE_WINDOW_MILLIS
        val window = points.filter { it.timestamp >= windowStart }
        if (window.size < 2) return 0.0
        var distance = 0.0
        window.zipWithNext().forEach { (from, to) ->
            val results = FloatArray(1)
            Location.distanceBetween(from.latitude, from.longitude, to.latitude, to.longitude, results)
            distance += results[0]
        }
        val duration = window.last().timestamp - window.first().timestamp
        return if (distance < 10.0 || duration <= 0L) 0.0 else duration / 1_000.0 / (distance / 1_000.0)
    }

    private companion object {
        const val MAX_ACCURACY_METERS = 25f
        const val MIN_SEGMENT_METERS = 2f
        const val MAX_GROUND_SPEED_MPS = 12f
        const val ROLLING_PACE_WINDOW_MILLIS = 30_000L
    }

    override fun onCleared() {
        fusedClient.removeLocationUpdates(locationCallback)
        ticker?.cancel()
        super.onCleared()
    }
}
