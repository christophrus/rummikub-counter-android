package com.example.rummikubcounter.data.repository

import androidx.lifecycle.LiveData
import com.example.rummikubcounter.data.local.AnalysisDao
import com.example.rummikubcounter.data.local.AnalysisResultEntity
import com.example.rummikubcounter.data.local.AnalysisResultWithTiles
import com.example.rummikubcounter.data.local.DetectedTileEntity
import com.example.rummikubcounter.model.AnalysisResult
import com.example.rummikubcounter.model.DetectedTile

class HistoryRepository(private val dao: AnalysisDao) {

    fun getAllResults(): LiveData<List<AnalysisResultWithTiles>> {
        return dao.getAllResultsWithTiles()
    }

    suspend fun saveResult(result: AnalysisResult, tiles: List<DetectedTile>) {
        val resultEntity = AnalysisResultEntity(
            timestamp = System.currentTimeMillis(),
            totalScore = result.totalScore,
            tileCount = result.tileCount,
            processingTimeMs = result.processingTimeMs,
            thumbnailPath = null
        )
        val resultId = dao.insertResult(resultEntity)

        val tileEntities = tiles.map { tile ->
            DetectedTileEntity(
                resultId = resultId,
                number = tile.number,
                confidence = tile.confidence,
                isJoker = tile.isJoker,
                x = tile.x,
                y = tile.y,
                width = tile.width,
                height = tile.height
            )
        }
        dao.insertTiles(tileEntities)
    }

    suspend fun deleteResult(resultId: Long) {
        dao.deleteResult(resultId)
    }
}
