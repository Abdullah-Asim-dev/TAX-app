package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.CameraScanView
import com.example.ui.components.DeductionAssistantDrawer
import com.example.ui.components.KineticLedgerView

@Composable
fun FreelanceTaxScreen(
    viewModel: FreelanceTaxViewModel = viewModel()
) {
    val filteredExpenses by viewModel.filteredExpenses.collectAsState()
    val metrics by viewModel.taxMetrics.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val scanState by viewModel.scanState.collectAsState()
    val isCameraModalOpen by viewModel.isCameraModalOpen.collectAsState()
    val activeDrawerResult by viewModel.activeDrawerResult.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        // Main Kinetic Ledger View
        KineticLedgerView(
            expenses = filteredExpenses,
            metrics = metrics,
            searchQuery = searchQuery,
            selectedFilter = selectedFilter,
            scanState = scanState,
            onSearchChange = viewModel::onSearchQueryChange,
            onFilterSelect = viewModel::onFilterSelect,
            onOpenScanner = viewModel::openCameraModal,
            onDeleteExpense = viewModel::deleteExpense,
            onQuickSampleScan = viewModel::scanSampleReceipt
        )

        // Camera View Overlay
        if (isCameraModalOpen) {
            BackHandler {
                viewModel.closeCameraModal()
            }
            CameraScanView(
                onImageCaptured = { bitmap ->
                    viewModel.processReceiptImage(bitmap)
                },
                onSampleSelected = { index ->
                    viewModel.closeCameraModal()
                    viewModel.scanSampleReceipt(index)
                },
                onDismiss = viewModel::closeCameraModal
            )
        }

        // AI Deduction Assistant Drawer / Modal
        if (activeDrawerResult != null) {
            BackHandler {
                viewModel.dismissDrawer()
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable { viewModel.dismissDrawer() },
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(modifier = Modifier.clickable(enabled = false) {}) {
                    DeductionAssistantDrawer(
                        result = activeDrawerResult!!,
                        onConfirm = viewModel::confirmAndAddToLedger,
                        onDismiss = viewModel::dismissDrawer
                    )
                }
            }
        }
    }
}
