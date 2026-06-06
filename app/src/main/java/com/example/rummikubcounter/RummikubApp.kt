package com.example.rummikubcounter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.rummikubcounter.ui.screens.AboutScreen
import com.example.rummikubcounter.ui.screens.CameraScreen
import com.example.rummikubcounter.ui.screens.HistoryScreen
import com.example.rummikubcounter.ui.screens.MainMenuScreen
import com.example.rummikubcounter.ui.screens.ResultScreen
import com.example.rummikubcounter.ui.screens.SettingsScreen
import com.example.rummikubcounter.viewmodel.AnalysisViewModel

object Routes {
    const val MAIN_MENU = "main_menu"
    const val CAMERA = "camera"
    const val RESULT = "result"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
}

@Composable
fun RummikubApp(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    viewModel: AnalysisViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Routes.MAIN_MENU,
        modifier = modifier
    ) {
        // --- Main Menu ---
        composable(Routes.MAIN_MENU) {
            MainMenuScreen(
                onNavigateToCamera = {
                    navController.navigate(Routes.CAMERA)
                },
                onNavigateToHistory = {
                    navController.navigate(Routes.HISTORY)
                },
                onNavigateToSettings = {
                    navController.navigate(Routes.SETTINGS)
                },
                onNavigateToAbout = {
                    navController.navigate(Routes.ABOUT)
                }
            )
        }

        // --- Camera ---
        composable(Routes.CAMERA) {
            CameraScreen(
                isLoading = uiState.isLoading,
                error = uiState.error,
                onImageCaptured = { bitmap ->
                    viewModel.analyze(bitmap)
                },
                onRetry = {
                    viewModel.reset()
                },
                onBack = {
                    viewModel.reset()
                    navController.popBackStack()
                }
            )

            // Navigate to result when analysis completes
            if (uiState.result != null && !uiState.isLoading) {
                androidx.compose.runtime.LaunchedEffect(uiState.result) {
                    navController.navigate(Routes.RESULT) {
                        launchSingleTop = true
                    }
                }
            }
        }

        // --- Result ---
        composable(Routes.RESULT) {
            val bitmap = uiState.originalBitmap
            val result = uiState.result
            if (bitmap != null && result != null) {
                ResultScreen(
                    bitmap = bitmap,
                    result = result,
                    onNewPhoto = {
                        viewModel.reset()
                        navController.popBackStack(Routes.CAMERA, inclusive = false)
                    },
                    onBack = {
                        viewModel.reset()
                        navController.popBackStack()
                    }
                )
            }
        }

        // --- History (placeholder for now) ---
        composable(Routes.HISTORY) {
            HistoryScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // --- Settings (placeholder for now) ---
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // --- About ---
        composable(Routes.ABOUT) {
            AboutScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
