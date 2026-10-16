package com.mirrorwalk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mirrorwalk.ui.RunScreen
import com.mirrorwalk.ui.RunSummaryScreen
import com.mirrorwalk.ui.RunsListScreen
import com.mirrorwalk.ui.GhostArScreen
import com.mirrorwalk.ui.theme.MirrorWalkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MirrorWalkApp() }
    }
}

@Composable
private fun MirrorWalkApp(viewModel: RunViewModel = viewModel()) {
    MirrorWalkTheme {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = "record") {
            composable("record") {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                RunScreen(
                    state = state,
                    onStartOrResume = { context -> viewModel.startOrResume(context) },
                    onPause = viewModel::pause,
                    onStop = viewModel::stop,
                    onNewRun = viewModel::clearForNewRun,
                    onShowSavedRun = { id -> navController.navigate("summary/$id") },
                    onOpenArGhost = { navController.navigate("ar-ghost") },
                    onGhostModeChanged = viewModel::setGhostModeEnabled,
                    onOpenRuns = { navController.navigate("runs") }
                )
            }
            composable("summary/{runId}") { entry ->
                val runId = entry.arguments?.getString("runId")?.toLongOrNull() ?: return@composable
                RunSummaryScreen(runId = runId, viewModel = viewModel, onBack = navController::popBackStack)
            }
            composable("ar-ghost") {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                // With ghost mode off this destination mounts no ARSceneView/session.
                if (state.ghostModeEnabled) GhostArScreen(runState = state, onBack = navController::popBackStack)
            }
            composable("runs") {
                RunsListScreen(
                    viewModel = viewModel,
                    onBack = navController::popBackStack,
                    onOpenRun = { id -> navController.navigate("summary/$id") }
                )
            }
        }
    }
}
