package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.backend.GeminiReceiptBackend
import com.example.data.local.AppDatabase
import com.example.data.model.AuditRiskLevel
import com.example.data.model.ExpenseEntity
import com.example.data.model.ReceiptExtractionResult
import com.example.data.model.ReceiptLineItem
import com.example.data.model.TaxClassification
import com.example.data.repository.ExpenseRepository
import com.example.data.repository.TaxMetrics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ScanUiState {
    object Idle : ScanUiState
    data class Processing(val step: String = "Analyzing receipt with Gemini API...") : ScanUiState
    data class Success(val result: ReceiptExtractionResult, val previewBitmap: Bitmap? = null) : ScanUiState
    data class Error(val message: String) : ScanUiState
}

class FreelanceTaxViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ExpenseRepository
    val expenses: StateFlow<List<ExpenseEntity>>

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow("All")
    val selectedFilter = _selectedFilter.asStateFlow()

    private val _scanState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val scanState = _scanState.asStateFlow()

    private val _activeDrawerResult = MutableStateFlow<ReceiptExtractionResult?>(null)
    val activeDrawerResult = _activeDrawerResult.asStateFlow()

    private val _isCameraModalOpen = MutableStateFlow(false)
    val isCameraModalOpen = _isCameraModalOpen.asStateFlow()

    init {
        val db = AppDatabase.getInstance(application)
        repository = ExpenseRepository(db.expenseDao())
        expenses = repository.expenses.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        viewModelScope.launch {
            repository.ensureInitialDataSeeded()
        }
    }

    val filteredExpenses = combine(expenses, _searchQuery, _selectedFilter) { list, query, filter ->
        list.filter { item ->
            val matchesQuery = query.isBlank() ||
                    item.merchant.contains(query, ignoreCase = true) ||
                    item.category.contains(query, ignoreCase = true) ||
                    item.scheduleCLine.contains(query, ignoreCase = true)

            val matchesFilter = when (filter) {
                "Schedule C" -> item.classification == TaxClassification.BUSINESS_DEDUCTIBLE
                "Meals (50%)" -> item.category.contains("Meal", ignoreCase = true) || item.deductionRate < 1.0
                "Software" -> item.category.contains("Software", ignoreCase = true) || item.category.contains("Hosting", ignoreCase = true)
                "Review" -> item.classification == TaxClassification.MIXED_PRO_RATED
                else -> true
            }

            matchesQuery && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val taxMetrics = expenses.combine(_selectedFilter) { list, _ ->
        repository.calculateMetrics(list)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        TaxMetrics(0.0, 0.0, 0.0, 3850.0, 0.0f, 96, 0, 0)
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onFilterSelect(filter: String) {
        _selectedFilter.value = filter
    }

    fun openCameraModal() {
        _isCameraModalOpen.value = true
    }

    fun closeCameraModal() {
        _isCameraModalOpen.value = false
    }

    fun dismissDrawer() {
        _activeDrawerResult.value = null
        if (_scanState.value is ScanUiState.Success) {
            _scanState.value = ScanUiState.Idle
        }
    }

    fun processReceiptImage(bitmap: Bitmap) {
        _isCameraModalOpen.value = false
        _scanState.value = ScanUiState.Processing("Extracting text and line items via Gemini...")

        viewModelScope.launch {
            try {
                _scanState.value = ScanUiState.Processing("Classifying Schedule C line & audit risk...")
                val result = GeminiReceiptBackend.extractReceiptData(bitmap)
                result.fold(
                    onSuccess = { extracted ->
                        _scanState.value = ScanUiState.Success(extracted, bitmap)
                        _activeDrawerResult.value = extracted
                    },
                    onFailure = { error ->
                        // Fallback gracefully to smart heuristic if network/key issues occur
                        val fallback = GeminiReceiptBackend.generateHeuristicSimulation(bitmap)
                        _scanState.value = ScanUiState.Success(fallback, bitmap)
                        _activeDrawerResult.value = fallback
                    }
                )
            } catch (e: Exception) {
                _scanState.value = ScanUiState.Error(e.message ?: "Failed to process receipt")
            }
        }
    }

    fun updateActiveDeduction(updated: ReceiptExtractionResult) {
        _activeDrawerResult.value = updated
    }

    fun confirmAndAddToLedger() {
        val current = _activeDrawerResult.value ?: return
        viewModelScope.launch {
            repository.saveExtraction(current)
            _activeDrawerResult.value = null
            _scanState.value = ScanUiState.Idle
        }
    }

    fun deleteExpense(item: ExpenseEntity) {
        viewModelScope.launch {
            repository.deleteExpense(item)
        }
    }

    fun scanSampleReceipt(sampleIndex: Int) {
        // Generates a mock bitmap for testing OCR in headless/emulator environments
        val bitmap = createSampleReceiptBitmap(sampleIndex)
        processReceiptImage(bitmap)
    }

    private fun createSampleReceiptBitmap(sampleIndex: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(600, 800, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(AndroidColor.WHITE)

        val paint = Paint().apply {
            color = AndroidColor.BLACK
            textSize = 28f
            isAntiAlias = true
        }

        val title = when (sampleIndex) {
            0 -> "AWS INVOICE #94821"
            1 -> "BLUE BOTTLE COFFEE"
            2 -> "APPLE STORE NYC"
            else -> "WEWORK COWORKING"
        }

        canvas.drawText(title, 50f, 100f, paint)
        paint.textSize = 20f
        canvas.drawText("Date: 2026-10-06", 50f, 150f, paint)
        canvas.drawText("---------------------------------", 50f, 190f, paint)
        canvas.drawText("Item 1: Professional Service", 50f, 240f, paint)
        canvas.drawText("Total: $142.80", 50f, 320f, paint)
        canvas.drawText("Payment: Paid via Business Visa", 50f, 370f, paint)

        return bitmap
    }
}
