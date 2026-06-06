package com.example.rummikubcounter.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rummikubcounter.data.local.AppDatabase
import com.example.rummikubcounter.data.local.AnalysisResultWithTiles
import com.example.rummikubcounter.data.repository.HistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: HistoryRepository by lazy {
        val db = AppDatabase.getInstance(application)
        HistoryRepository(db.analysisDao())
    }

    val allResults: Flow<List<AnalysisResultWithTiles>> = repository.getAllResults()

    fun deleteEntry(resultId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteResult(resultId)
        }
    }
}
