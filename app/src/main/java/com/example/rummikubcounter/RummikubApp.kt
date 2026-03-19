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
import com.example.rummikubcounter.ui.screens.CameraScreen
import com.example.rummikubcounter.ui.screens.ResultScreen
import com.example.rummikubcounter.viewmodel.AnalysisViewModel

object Routes {
    const val CAMERA = "camera"
    const val RESULT = "result"
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
        startDestination = Routes.CAMERA,
        modifier = modifier
    ) {
        composable(Routes.CAMERA) {
            CameraScreen(
                isLoading = uiState.isLoading,
                onImageCaptured = { bitmap ->
                    viewModel.analyze(bitmap)
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
                    }
                )
            }
        }
    }
}
