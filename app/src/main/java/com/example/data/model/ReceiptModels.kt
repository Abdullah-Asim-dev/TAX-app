package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TaxClassification {
    BUSINESS_DEDUCTIBLE,
    PERSONAL_NON_DEDUCTIBLE,
    MIXED_PRO_RATED
}

enum class AuditRiskLevel {
    LOW,
    MEDIUM,
    HIGH
}

data class ReceiptLineItem(
    val description: String,
    val amount: Double
)

data class ReceiptExtractionResult(
    val merchant: String,
    val date: String,
    val totalAmount: Double,
    val taxAmount: Double = 0.0,
    val currency: String = "USD",
    val category: String,
    val scheduleCLine: String,
    val classification: TaxClassification,
    val deductionRate: Double, // 1.0 = 100%, 0.5 = 50%
    val taxSavingsEstimate: Double,
    val auditRisk: AuditRiskLevel,
    val auditRationale: String,
    val confidenceScore: Double,
    val lineItems: List<ReceiptLineItem> = emptyList(),
    val rawJson: String = ""
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val merchant: String,
    val date: String,
    val totalAmount: Double,
    val taxAmount: Double,
    val currency: String,
    val category: String,
    val scheduleCLine: String,
    val classification: TaxClassification,
    val deductionRate: Double,
    val taxSavingsEstimate: Double,
    val auditRisk: AuditRiskLevel,
    val auditRationale: String,
    val confidenceScore: Double,
    val lineItemsSummary: String, // serialized or formatted
    val timestamp: Long = System.currentTimeMillis()
)
