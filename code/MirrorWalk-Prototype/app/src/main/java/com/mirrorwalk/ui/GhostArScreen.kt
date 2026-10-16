package com.mirrorwalk.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.Plane
import com.google.ar.core.TrackingState
import com.mirrorwalk.RunStatus
import com.mirrorwalk.RunUiState
import com.mirrorwalk.data.HardcodedGhostRunSource
import com.mirrorwalk.data.RecordedPoint
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.math.Position
import io.github.sceneview.node.CapsuleNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import kotlin.math.cos

/** The AR ghost is driven by the active run clock, not AR frame count. */
@Composable
fun GhostArScreen(runState: RunUiState, onBack: () -> Unit) {
    val ghostRun = remember { HardcodedGhostRunSource.loadGhostRun() }
    val route = remember(ghostRun) { LocalGhostRoute.from(ghostRun.points) }
    val engine = rememberEngine()
    val materialLoader = rememberMaterialLoader(engine)
    val ghostMaterial = remember(materialLoader) {
        materialLoader.createColorInstance(Color(0x8878F2C2), metallic = 0f, roughness = 0.35f)
    }
    var anchor by remember { mutableStateOf<Anchor?>(null) }
    var renderedElapsedMillis by remember { mutableLongStateOf(runState.elapsedMillis) }

    // RunUiState updates periodically for the text UI; this loop interpolates the
    // same run clock on every rendered frame so the AR transform is smooth.
    LaunchedEffect(runState.status, runState.segmentStartedAt, runState.elapsedMillis) {
        val segmentStart = runState.segmentStartedAt
        if (runState.status != RunStatus.RUNNING || segmentStart == null) {
            renderedElapsedMillis = runState.elapsedMillis
            return@LaunchedEffect
        }
        val elapsedAtFrameLoopStart = runState.elapsedMillis
        while (true) {
            withFrameNanos { }
            renderedElapsedMillis = elapsedAtFrameLoopStart + (System.currentTimeMillis() - segmentStart)
        }
    }

    // GPS and ARCore don't share a reliable geographic coordinate space. Time is the sync contract.
    val ghostTime = renderedElapsedMillis.coerceIn(0L, route.durationMillis)
    val position = route.positionAt(ghostTime)
    val ghostDistance = route.distanceAt(ghostTime)

    Box(Modifier.fillMaxSize()) {
        ARSceneView(
            modifier = Modifier.fillMaxSize(), engine = engine, planeRenderer = true,
            sessionConfiguration = { _, config ->
                config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
                config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
            },
            onSessionUpdated = { session, _ ->
                if (anchor == null) anchor = session.getAllTrackables(Plane::class.java)
                    .firstOrNull { it.trackingState == TrackingState.TRACKING && it.type == Plane.Type.HORIZONTAL_UPWARD_FACING }
                    ?.let { it.createAnchor(it.centerPose) }
            }
        ) {
            anchor?.let { groundAnchor -> AnchorNode(anchor = groundAnchor) {
                CapsuleNode(radius = 0.16f, height = 1.35f, materialInstance = ghostMaterial, position = Position(position.x, 0.835f, position.z))
            } }
        }

        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(14.dp)) {
            Icon(Icons.Default.ArrowBack, "Back to live run")
        }
        Card(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 18.dp, start = 64.dp, end = 18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text("GHOST PREVIEW", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                Text(when {
                    runState.status == RunStatus.IDLE -> "Start a run first, then open this view."
                    anchor == null -> "Move slowly over an open floor…"
                    else -> ghostRun.label
                }, style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (runState.status != RunStatus.IDLE) Card(
            modifier = Modifier.align(Alignment.BottomCenter).padding(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f))
        ) {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
                Text("LIVE COMPARISON", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelMedium)
                Text("YOU  ${formatDistance(runState.totalDistanceMeters)}  |  ${formatPace(runState.currentPaceSecondsPerKm)}", style = MaterialTheme.typography.bodyMedium)
                Text("GHOST  ${formatDistance(ghostDistance)}  |  ${formatPace(paceAt(ghostDistance, ghostTime))}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private data class LocalPosition(val x: Float, val z: Float)
private data class TimedPosition(val time: Long, val position: LocalPosition, val distance: Double)

private class LocalGhostRoute private constructor(private val samples: List<TimedPosition>, val durationMillis: Long) {
    fun positionAt(time: Long): LocalPosition {
        val next = nextIndex(time)
        if (next == 0) return samples.first().position
        val a = samples[next - 1]; val b = samples[next]; val f = fraction(time, a, b)
        return LocalPosition(a.position.x + (b.position.x - a.position.x) * f, a.position.z + (b.position.z - a.position.z) * f)
    }
    fun distanceAt(time: Long): Double {
        val next = nextIndex(time)
        if (next == 0) return 0.0
        val a = samples[next - 1]; val b = samples[next]
        return a.distance + (b.distance - a.distance) * fraction(time, a, b)
    }
    private fun nextIndex(time: Long) = samples.indexOfFirst { it.time >= time }.let { if (it == -1) samples.lastIndex else it }
    private fun fraction(time: Long, a: TimedPosition, b: TimedPosition) = ((time - a.time).toFloat() / (b.time - a.time).coerceAtLeast(1L)).coerceIn(0f, 1f)
    companion object {
        private const val AR_DEMO_SCALE = 0.035
        fun from(points: List<RecordedPoint>): LocalGhostRoute {
            require(points.size >= 2)
            val origin = points.first(); val latitudeRadians = Math.toRadians(origin.latitude); var distance = 0.0
            val samples = points.mapIndexed { index, point ->
                if (index > 0) {
                    val earlier = points[index - 1]; val result = FloatArray(1)
                    android.location.Location.distanceBetween(earlier.latitude, earlier.longitude, point.latitude, point.longitude, result)
                    distance += result[0]
                }
                val north = (point.latitude - origin.latitude) * 111_320.0
                val east = (point.longitude - origin.longitude) * 111_320.0 * cos(latitudeRadians)
                TimedPosition(point.timestamp - origin.timestamp, LocalPosition((east * AR_DEMO_SCALE).toFloat(), (-north * AR_DEMO_SCALE).toFloat()), distance)
            }
            return LocalGhostRoute(samples, samples.last().time.coerceAtLeast(1L))
        }
    }
}

private fun formatDistance(meters: Double) = String.format(java.util.Locale.US, "%.2f km", meters / 1_000.0)
private fun formatPace(secondsPerKm: Double): String {
    if (secondsPerKm <= 0.0 || !secondsPerKm.isFinite()) return "--:-- /km"
    val seconds = secondsPerKm.toInt()
    return String.format(java.util.Locale.US, "%d:%02d /km", seconds / 60, seconds % 60)
}
private fun paceAt(distanceMeters: Double, elapsedMillis: Long) = if (distanceMeters < 10.0) 0.0 else elapsedMillis / 1_000.0 / (distanceMeters / 1_000.0)
