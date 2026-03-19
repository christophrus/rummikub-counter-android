package com.example.rummikubcounter.model

data class AnalysisResult(
    val tiles: List<DetectedTile>,
    val totalScore: Int,
    val tileCount: Int,
    val processingTimeMs: Long
)
