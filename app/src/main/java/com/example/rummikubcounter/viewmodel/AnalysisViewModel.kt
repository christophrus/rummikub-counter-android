package com.example.rummikubcounter.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rummikubcounter.data.SettingsDataStore
import com.example.rummikubcounter.data.local.AppDatabase
import com.example.rummikubcounter.data.repository.HistoryRepository
import com.example.rummikubcounter.ml.ImagePreprocessor
import com.example.rummikubcounter.ml.NmsProcessor
import com.example.rummikubcounter.ml.OrientationDetector
import com.example.rummikubcounter.ml.OrientationPreprocessor
import com.example.rummikubcounter.ml.YoloDetector
import com.example.rummikubcounter.model.AnalysisResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

data class AnalysisUiState(
    val isLoading: Boolean = false,
    val result: AnalysisResult? = null,
    val originalBitmap: Bitmap? = null,
    val error: String? = null
)

class AnalysisViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AnalysisUiState())
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    private val detector: YoloDetector by lazy { YoloDetector(application) }
    private val orientationDetector: OrientationDetector by lazy { OrientationDetector(application) }
    private val historyRepository: HistoryRepository by lazy {
        val db = AppDatabase.getInstance(application)
        HistoryRepository(db.analysisDao())
    }
    private val settingsDataStore: SettingsDataStore by lazy {
        SettingsDataStore(application)
    }

    fun analyze(bitmap: Bitmap) {
        // Read current confidence threshold from settings
        val confThreshold = runBlocking {
            settingsDataStore.confidenceThreshold.first()
        }

        viewModelScope.launch(Dispatchers.Default) {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val startTime = System.currentTimeMillis()

                // Downscale large images to prevent OOM
                val safeBitmap = ImagePreprocessor.downscaleIfNeeded(bitmap)

                // Step 1: Detect and correct orientation
                val orientationInput = OrientationPreprocessor.preprocess(safeBitmap)
                val detectedDegrees = orientationDetector.detect(orientationInput)
                val correctionDegrees = orientationDetector.correctionDegrees(detectedDegrees)
                val orientedBitmap = if (correctionDegrees != 0) {
                    ImagePreprocessor.rotateBitmap(safeBitmap, correctionDegrees)
                } else {
                    safeBitmap
                }

                // Step 2: YOLO tile detection
                val (inputArray, letterboxInfo) = ImagePreprocessor.preprocess(orientedBitmap)
                val rawOutput = detector.detect(inputArray)
                val tiles = NmsProcessor.postProcess(
                    rawOutput, letterboxInfo, orientedBitmap.width, orientedBitmap.height,
                    confThreshold = confThreshold
                )
                val elapsed = System.currentTimeMillis() - startTime

                val totalScore = tiles.sumOf {
                    if (it.isJoker) 20 else (it.number ?: 0)
                }

                val result = AnalysisResult(
                    tiles = tiles,
                    totalScore = totalScore,
                    tileCount = tiles.size,
                    processingTimeMs = elapsed
                )

                // Save to history
                historyRepository.saveResult(result, tiles)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        originalBitmap = orientedBitmap,
                        result = result
                    )
                }
            } catch (e: Exception) {
                Log.e("AnalysisViewModel", "Analysis failed", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Unbekannter Fehler"
                    )
                }
            }
        }
    }

    fun reset() {
        _uiState.update { AnalysisUiState() }
    }

    override fun onCleared() {
        super.onCleared()
        orientationDetector.close()
        detector.close()
    }
}
