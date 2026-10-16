package com.mirrorwalk.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.clickable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mirrorwalk.RunStatus
import com.mirrorwalk.RunUiState
import com.mirrorwalk.RunViewModel
import com.mirrorwalk.data.RunEntity
import com.mirrorwalk.ui.theme.Danger
import com.mirrorwalk.ui.theme.Lime
import com.mirrorwalk.ui.theme.Mint
import com.mirrorwalk.ui.theme.MirrorWalkBrandFont
import com.mirrorwalk.ui.theme.Muted
import java.util.Locale
import kotlin.math.roundToInt
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory.lineCap
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineJoin
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.layers.Property.LINE_CAP_ROUND
import org.maplibre.android.style.layers.Property.LINE_JOIN_ROUND
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

@Composable
fun RunScreen(
    state: RunUiState,
    onStartOrResume: (Context) -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onNewRun: () -> Unit,
    onShowSavedRun: (Long) -> Unit,
    onOpenArGhost: () -> Unit,
    onGhostModeChanged: (Boolean) -> Unit,
    onOpenRuns: () -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(hasLocationPermission(context)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        hasPermission = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true || grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (hasPermission) onStartOrResume(context)
    }
    val requestStart = {
        if (hasPermission) onStartOrResume(context)
        else launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { BottomBar(onAdd = if (state.status == RunStatus.RUNNING) onPause else requestStart, onRuns = onOpenRuns) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(18.dp))
            Text(
                "MIRRORWALK",
                color = Mint,
                style = MaterialTheme.typography.labelLarge,
                fontFamily = MirrorWalkBrandFont,
                letterSpacing = 2.sp
            )
            Text(
                when (state.status) {
                    RunStatus.RUNNING -> "Keep moving."
                    RunStatus.PAUSED -> "Run paused"
                    RunStatus.FINISHED -> "Run saved"
                    RunStatus.IDLE -> "Ready when you are"
                },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            if (state.status == RunStatus.IDLE) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Race ghost", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Show your past run in AR during this session",
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Switch(checked = state.ghostModeEnabled, onCheckedChange = onGhostModeChanged)
                    }
                }
            } else if (state.ghostModeEnabled && (state.status == RunStatus.RUNNING || state.status == RunStatus.PAUSED)) {
                androidx.compose.material3.TextButton(onClick = onOpenArGhost) { Text("Open AR ghost preview") }
            }
            Spacer(Modifier.height(16.dp))
            RouteMap(points = state.points, myLocation = hasPermission, modifier = Modifier.fillMaxWidth().height(270.dp))
            Spacer(Modifier.height(16.dp))
            StatsCard(state)
            state.error?.let { Text(it, color = Danger, modifier = Modifier.padding(top = 8.dp)) }
            Spacer(Modifier.weight(1f))
            Controls(state, requestStart, onPause, onStop, onNewRun, onShowSavedRun)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun RouteMap(points: List<com.mirrorwalk.data.RecordedPoint>, myLocation: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).also { it.onCreate(null) }
    }
    var mapAndStyle by remember { mutableStateOf<Pair<MapLibreMap, org.maplibre.android.maps.Style>?>(null) }
    val path = remember(points) { points.map { LatLng(it.latitude, it.longitude) } }

    DisposableEffect(mapView, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        // Lifecycle observers do not receive past events. Compose can mount this
        // MapView after the Activity is already resumed, so start it explicitly.
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) mapView.onStart()
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onResume()
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }
    LaunchedEffect(mapAndStyle, path) {
        val (map, style) = mapAndStyle ?: return@LaunchedEffect
        // MapLibre's native GeoJsonSource implementation cannot accept a null
        // Feature. A new run has zero or one location point, so leave the map
        // source untouched until there is a real line to render.
        if (path.size < 2) return@LaunchedEffect
        val feature = Feature.fromGeometry(
            LineString.fromLngLats(path.map { Point.fromLngLat(it.longitude, it.latitude) })
        )
        val source = style.getSourceAs<GeoJsonSource>(MAP_PATH_SOURCE) ?: GeoJsonSource(MAP_PATH_SOURCE, feature).also {
            style.addSource(it)
            style.addLayer(
                LineLayer(MAP_PATH_LAYER, MAP_PATH_SOURCE).withProperties(
                    lineColor("#78F2C2"), lineWidth(6f), lineCap(LINE_CAP_ROUND), lineJoin(LINE_JOIN_ROUND)
                )
            )
        }
        source.setGeoJson(feature)
        path.lastOrNull()?.let { map.animateCamera(CameraUpdateFactory.newLatLngZoom(it, 17.0)) }
    }
    Card(modifier = modifier, shape = MaterialTheme.shapes.large) {
        AndroidView(
            factory = {
                mapView.apply {
                    getMapAsync { map ->
                        map.moveCamera(CameraUpdateFactory.newCameraPosition(
                            CameraPosition.Builder().target(LatLng(12.9716, 77.5946)).zoom(13.0).build()
                        ))
                        map.setStyle(OPEN_FREE_MAP_STYLE) { style -> mapAndStyle = map to style }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

private const val OPEN_FREE_MAP_STYLE = "https://tiles.openfreemap.org/styles/liberty"
private const val MAP_PATH_SOURCE = "mirrorwalk-recorded-path"
private const val MAP_PATH_LAYER = "mirrorwalk-recorded-path-line"

@Composable
private fun StatsCard(state: RunUiState) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            Stat("DISTANCE", String.format(Locale.US, "%.2f km", state.totalDistanceMeters / 1000.0))
            Stat("PACE", formatPace(state.currentPaceSecondsPerKm))
            Stat("TIME", formatTime(state.elapsedMillis))
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Muted, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp)
        Spacer(Modifier.height(6.dp))
        Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun Controls(
    state: RunUiState, onStart: () -> Unit, onPause: () -> Unit, onStop: () -> Unit,
    onNewRun: () -> Unit, onShowSavedRun: (Long) -> Unit
) {
    when (state.status) {
        RunStatus.IDLE -> WideButton("Start run", onStart)
        RunStatus.RUNNING -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onPause, modifier = Modifier.weight(1f).height(54.dp)) { Text("Pause") }
            Button(onClick = onStop, modifier = Modifier.weight(1f).height(54.dp), colors = ButtonDefaults.buttonColors(containerColor = Danger, contentColor = MaterialTheme.colorScheme.background)) { Text("Stop") }
        }
        RunStatus.PAUSED -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onStop, modifier = Modifier.weight(1f).height(54.dp)) { Text("Stop") }
            Button(onClick = onStart, modifier = Modifier.weight(1f).height(54.dp)) { Text("Resume") }
        }
        RunStatus.FINISHED -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            state.savedRunId?.let { WideButton("View saved route") { onShowSavedRun(it) } }
            OutlinedButton(onClick = onNewRun, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Record another run") }
        }
    }
}

@Composable
private fun WideButton(label: String, onClick: () -> Unit) = Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text(label, fontWeight = FontWeight.Bold) }

@Composable
private fun BottomBar(onAdd: () -> Unit, onRuns: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().navigationBarsPadding().height(72.dp).padding(horizontal = 36.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
    ) {
        Text("TRACK", color = Mint, style = MaterialTheme.typography.labelMedium)
        FilledIconButton(onClick = onAdd, modifier = Modifier.size(54.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = Lime, contentColor = MaterialTheme.colorScheme.background)) { Icon(Icons.Default.Add, "Start or pause run") }
        Row(modifier = Modifier.clickable(onClick = onRuns), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.LocationOn, null, tint = Muted); Spacer(Modifier.width(4.dp)); Text("RUNS", color = Muted, style = MaterialTheme.typography.labelMedium) }
    }
}

@Composable
fun RunSummaryScreen(runId: Long, viewModel: RunViewModel, onBack: () -> Unit) {
    val run by viewModel.observeRun(runId).collectAsStateWithLifecycle(initialValue = null)
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                Text("Saved route", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            if (run == null) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Loading route…", color = Muted) }
            else {
                val points = remember(run!!.pathJson) { viewModel.decodePath(run!!.pathJson) }
                RouteMap(points, myLocation = false, modifier = Modifier.fillMaxWidth().weight(1f))
                Spacer(Modifier.height(16.dp))
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.SpaceAround) {
                        Stat("DISTANCE", String.format(Locale.US, "%.2f km", run!!.totalDistanceMeters / 1000.0))
                        Stat("AVG PACE", formatPace(run!!.averagePaceSecondsPerKm))
                    }
                }
            }
        }
    }
}

@Composable
fun RunsListScreen(viewModel: RunViewModel, onBack: () -> Unit, onOpenRun: (Long) -> Unit) {
    val runs by viewModel.observeRuns().collectAsStateWithLifecycle(initialValue = emptyList())
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                Text("Your runs", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            if (runs.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No recorded runs yet.", color = Muted) }
            } else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 12.dp)) {
                items(runs, key = { it.id }) { run -> SavedRunItem(run, onOpenRun, viewModel::deleteRun) }
            }
        }
    }
}

@Composable
private fun SavedRunItem(run: RunEntity, onOpenRun: (Long) -> Unit, onDeleteRun: (Long) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onOpenRun(run.id) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT).format(java.util.Date(run.startedAt)), fontWeight = FontWeight.Bold)
                Text("${String.format(Locale.US, "%.2f km", run.totalDistanceMeters / 1000.0)}  ·  ${formatPace(run.averagePaceSecondsPerKm)}", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = { onDeleteRun(run.id) }) { Icon(Icons.Default.Delete, "Delete run", tint = Danger) }
        }
    }
}


private fun hasLocationPermission(context: Context) = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
private fun formatTime(millis: Long): String { val seconds = millis / 1000; return String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60) }
private fun formatPace(seconds: Double): String { if (seconds <= 0 || !seconds.isFinite()) return "--:--"; return String.format(Locale.US, "%d:%02d /km", (seconds / 60).toInt(), (seconds.roundToInt() % 60)) }
