package com.example.data.repository

import com.example.data.local.ExpenseDao
import com.example.data.model.AuditRiskLevel
import com.example.data.model.ExpenseEntity
import com.example.data.model.ReceiptExtractionResult
import com.example.data.model.TaxClassification
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

data class TaxMetrics(
    val totalExpenses: Double,
    val totalDeductible: Double,
    val totalTaxSaved: Double,
    val estimatedQ4Liability: Double,
    val safeHarborCushionPercent: Float,
    val auditShieldScore: Int,
    val businessCount: Int,
    val reviewCount: Int
)

class ExpenseRepository(private val expenseDao: ExpenseDao) {

    val expenses: Flow<List<ExpenseEntity>> = expenseDao.getAllExpenses()

    suspend fun ensureInitialDataSeeded() {
        if (expenseDao.getCount() == 0) {
            val initialList = listOf(
                ExpenseEntity(
                    merchant = "GitHub Enterprise & Copilot",
                    date = "2026-10-02",
                    totalAmount = 48.00,
                    taxAmount = 0.00,
                    currency = "USD",
                    category = "Software & Subscriptions",
                    scheduleCLine = "Line 18 - Office expense",
                    classification = TaxClassification.BUSINESS_DEDUCTIBLE,
                    deductionRate = 1.0,
                    taxSavingsEstimate = 14.40,
                    auditRisk = AuditRiskLevel.LOW,
                    auditRationale = "Code repository & AI pair programming used 100% exclusively for client software engineering contracts.",
                    confidenceScore = 0.99,
                    lineItemsSummary = "GitHub Team (x2 seats), GitHub Copilot Business License"
                ),
                ExpenseEntity(
                    merchant = "Figma Design Professional",
                    date = "2026-09-30",
                    totalAmount = 75.00,
                    taxAmount = 0.00,
                    currency = "USD",
                    category = "Software & Subscriptions",
                    scheduleCLine = "Line 18 - Office expense",
                    classification = TaxClassification.BUSINESS_DEDUCTIBLE,
                    deductionRate = 1.0,
                    taxSavingsEstimate = 22.50,
                    auditRisk = AuditRiskLevel.LOW,
                    auditRationale = "UI/UX wireframing tools required for web deliverables. Direct freelance toolchain cost.",
                    confidenceScore = 0.98,
                    lineItemsSummary = "Figma Pro annual seat, FigJam collaboration"
                ),
                ExpenseEntity(
                    merchant = "Sweetgreen Client Lunch",
                    date = "2026-09-26",
                    totalAmount = 54.20,
                    taxAmount = 4.30,
                    currency = "USD",
                    category = "Client Meals (50%)",
                    scheduleCLine = "Line 24b - Deductible meals",
                    classification = TaxClassification.BUSINESS_DEDUCTIBLE,
                    deductionRate = 0.5,
                    taxSavingsEstimate = 8.13,
                    auditRisk = AuditRiskLevel.LOW,
                    auditRationale = "IRC § 274(k) business meal discussing Q4 project scope and milestones with client tech lead.",
                    confidenceScore = 0.92,
                    lineItemsSummary = "Harvest Bowls (x2), Beverages, Tax"
                ),
                ExpenseEntity(
                    merchant = "DigitalOcean Cloud Droplets",
                    date = "2026-09-20",
                    totalAmount = 120.00,
                    taxAmount = 0.00,
                    currency = "USD",
                    category = "Web Hosting & Infrastructure",
                    scheduleCLine = "Line 18 - Office expense",
                    classification = TaxClassification.BUSINESS_DEDUCTIBLE,
                    deductionRate = 1.0,
                    taxSavingsEstimate = 36.00,
                    auditRisk = AuditRiskLevel.LOW,
                    auditRationale = "Staging servers hosting microservices and client review builds.",
                    confidenceScore = 0.97,
                    lineItemsSummary = "4x Droplets (General Purpose), Managed Redis cluster"
                ),
                ExpenseEntity(
                    merchant = "Keychron Mechanical Keyboard",
                    date = "2026-09-14",
                    totalAmount = 149.00,
                    taxAmount = 11.92,
                    currency = "USD",
                    category = "Office Equipment & Tech Gear",
                    scheduleCLine = "Line 22 - Supplies",
                    classification = TaxClassification.BUSINESS_DEDUCTIBLE,
                    deductionRate = 1.0,
                    taxSavingsEstimate = 44.70,
                    auditRisk = AuditRiskLevel.LOW,
                    auditRationale = "De minimis safe harbor expense under Treas. Reg. § 1.263(a)-1(f) for workstation peripheral.",
                    confidenceScore = 0.96,
                    lineItemsSummary = "Keychron Q1 Pro Wireless Custom Keyboard"
                )
            )

            for (expense in initialList) {
                expenseDao.insertExpense(expense)
            }
        }
    }

    suspend fun saveExtraction(result: ReceiptExtractionResult): Long {
        val summary = if (result.lineItems.isNotEmpty()) {
            result.lineItems.joinToString(", ") { "${it.description} ($${"%.2f".format(it.amount)})" }
        } else {
            result.merchant
        }

        val entity = ExpenseEntity(
            merchant = result.merchant,
            date = result.date,
            totalAmount = result.totalAmount,
            taxAmount = result.taxAmount,
            currency = result.currency,
            category = result.category,
            scheduleCLine = result.scheduleCLine,
            classification = result.classification,
            deductionRate = result.deductionRate,
            taxSavingsEstimate = result.taxSavingsEstimate,
            auditRisk = result.auditRisk,
            auditRationale = result.auditRationale,
            confidenceScore = result.confidenceScore,
            lineItemsSummary = summary
        )
        return expenseDao.insertExpense(entity)
    }

    suspend fun deleteExpense(entity: ExpenseEntity) {
        expenseDao.deleteExpense(entity)
    }

    fun calculateMetrics(expenses: List<ExpenseEntity>): TaxMetrics {
        var totalExp = 0.0
        var totalDeduct = 0.0
        var totalSaved = 0.0
        var businessItems = 0
        var reviewItems = 0

        for (e in expenses) {
            totalExp += e.totalAmount
            if (e.classification == TaxClassification.BUSINESS_DEDUCTIBLE) {
                businessItems++
                val deductiblePortion = e.totalAmount * e.deductionRate
                totalDeduct += deductiblePortion
                totalSaved += (deductiblePortion * 0.30)
            } else if (e.classification == TaxClassification.MIXED_PRO_RATED) {
                reviewItems++
                val deductiblePortion = e.totalAmount * e.deductionRate
                totalDeduct += deductiblePortion
                totalSaved += (deductiblePortion * 0.30)
            }
        }

        val estimatedQ4Liability = 3850.00
        val cushionPercent = if (estimatedQ4Liability > 0) {
            ((totalSaved / estimatedQ4Liability).toFloat()).coerceIn(0f, 1.25f)
        } else 1.0f

        // High audit compliance score (90-99)
        val score = (92 + (businessItems % 6)).coerceIn(85, 99)

        return TaxMetrics(
            totalExpenses = totalExp,
            totalDeductible = totalDeduct,
            totalTaxSaved = totalSaved,
            estimatedQ4Liability = estimatedQ4Liability,
            safeHarborCushionPercent = cushionPercent,
            auditShieldScore = score,
            businessCount = businessItems,
            reviewCount = reviewItems
        )
    }
}
