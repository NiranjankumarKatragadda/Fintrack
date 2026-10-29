package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Expense(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val type: String = "EXPENSE", // "EXPENSE" or "INCOME"
    val category: String = "Food & Dining",
    val date: String = "",
    val note: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        return mapOf(
            "userId" to userId,
            "title" to title,
            "amount" to amount,
            "type" to type,
            "category" to category,
            "date" to date,
            "note" to note,
            "createdAt" to (createdAt ?: FieldValue.serverTimestamp())
        ).filterValues {
            it != null && (it !is String || it.isNotEmpty() || it == "note" || it == "date")
        }
    }
}

@IgnoreExtraProperties
data class Budget(
    @DocumentId val id: String = "",
    val userId: String = "",
    val category: String = "",
    val monthlyLimit: Double = 0.0,
    val alertThresholdPercent: Int = 80,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SavingsGoal(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val targetAmount: Double = 0.0,
    val currentAmount: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class BudgetAlertSeverity {
    SAFE,       // < Threshold%
    WARNING,    // >= Threshold% and < 100%
    EXCEEDED    // >= 100%
}

data class CategoryBudgetAlert(
    val category: String,
    val spent: Double,
    val limit: Double,
    val percentage: Int,
    val thresholdPercent: Int,
    val severity: BudgetAlertSeverity,
    val alertMessage: String
)

object FinanceCategories {
    val EXPENSE_CATEGORIES = listOf(
        "Food & Dining",
        "Shopping",
        "Transportation",
        "Bills & Utilities",
        "Entertainment",
        "Health & Fitness",
        "Travel",
        "Education",
        "Personal Care",
        "Other"
    )

    val INCOME_CATEGORIES = listOf(
        "Salary",
        "Freelance",
        "Investments",
        "Rental",
        "Gifts",
        "Other Income"
    )
}
