package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.OperationType
import com.example.data.handleFirestoreError
import com.example.data.model.Budget
import com.example.data.model.Expense
import com.example.data.model.SavingsGoal
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FinanceRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    // Observe real-time expenses for authenticated user
    fun observeExpenses(userId: String): Flow<List<Expense>> {
        val path = "users/$userId/expenses"
        return db.collection("users").document(userId).collection("expenses")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Expense::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
            }
            .catch { error ->
                if (error !is CancellationException) {
                    handleFirestoreError(error as? Exception ?: Exception(error), OperationType.LIST, path)
                }
                throw error
            }
    }

    // Observe real-time category budgets
    fun observeBudgets(userId: String): Flow<List<Budget>> {
        val path = "users/$userId/budgets"
        return db.collection("users").document(userId).collection("budgets")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Budget::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
            }
            .catch { error ->
                if (error !is CancellationException) {
                    handleFirestoreError(error as? Exception ?: Exception(error), OperationType.LIST, path)
                }
                throw error
            }
    }

    // Observe real-time savings goals
    fun observeGoals(userId: String): Flow<List<SavingsGoal>> {
        val path = "users/$userId/goals"
        return db.collection("users").document(userId).collection("goals")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(SavingsGoal::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
            }
            .catch { error ->
                if (error !is CancellationException) {
                    handleFirestoreError(error as? Exception ?: Exception(error), OperationType.LIST, path)
                }
                throw error
            }
    }

    // Add new expense
    suspend fun addExpense(userId: String, expense: Expense): Result<String> {
        val docRef = db.collection("users").document(userId).collection("expenses").document()
        val payload = mapOf(
            "userId" to userId,
            "title" to expense.title.trim(),
            "amount" to expense.amount,
            "type" to expense.type,
            "category" to expense.category.trim(),
            "date" to expense.date.ifBlank { "Today" },
            "note" to expense.note.trim(),
            "createdAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    // Update existing expense
    suspend fun updateExpense(userId: String, expense: Expense): Result<Unit> {
        require(expense.id.isNotBlank()) { "Expense ID cannot be blank" }
        requireNotNull(expense.createdAt) { "Expense createdAt cannot be null" }
        val docRef = db.collection("users").document(userId).collection("expenses").document(expense.id)
        val payload = mapOf(
            "userId" to userId,
            "title" to expense.title.trim(),
            "amount" to expense.amount,
            "type" to expense.type,
            "category" to expense.category.trim(),
            "date" to expense.date,
            "note" to expense.note.trim(),
            "createdAt" to expense.createdAt,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    // Delete expense
    suspend fun deleteExpense(userId: String, expenseId: String): Result<Unit> {
        require(expenseId.isNotBlank()) { "Expense ID cannot be blank" }
        val docRef = db.collection("users").document(userId).collection("expenses").document(expenseId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // Set or update budget
    suspend fun saveBudget(userId: String, budget: Budget): Result<String> {
        val docRef = if (budget.id.isNotBlank()) {
            db.collection("users").document(userId).collection("budgets").document(budget.id)
        } else {
            db.collection("users").document(userId).collection("budgets").document()
        }

        val isUpdate = budget.id.isNotBlank() && budget.createdAt != null
        val payload = if (isUpdate) {
            mapOf(
                "userId" to userId,
                "category" to budget.category.trim(),
                "monthlyLimit" to budget.monthlyLimit,
                "alertThresholdPercent" to budget.alertThresholdPercent,
                "createdAt" to budget.createdAt,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        } else {
            mapOf(
                "userId" to userId,
                "category" to budget.category.trim(),
                "monthlyLimit" to budget.monthlyLimit,
                "alertThresholdPercent" to budget.alertThresholdPercent,
                "createdAt" to FieldValue.serverTimestamp()
            )
        }

        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val op = if (isUpdate) OperationType.UPDATE else OperationType.CREATE
            handleFirestoreError(e, op, docRef.path)
            Result.failure(e)
        }
    }

    // Delete budget
    suspend fun deleteBudget(userId: String, budgetId: String): Result<Unit> {
        require(budgetId.isNotBlank()) { "Budget ID cannot be blank" }
        val docRef = db.collection("users").document(userId).collection("budgets").document(budgetId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // Add or update savings goal
    suspend fun saveGoal(userId: String, goal: SavingsGoal): Result<String> {
        val docRef = if (goal.id.isNotBlank()) {
            db.collection("users").document(userId).collection("goals").document(goal.id)
        } else {
            db.collection("users").document(userId).collection("goals").document()
        }

        val isUpdate = goal.id.isNotBlank() && goal.createdAt != null
        val payload = if (isUpdate) {
            mapOf(
                "userId" to userId,
                "title" to goal.title.trim(),
                "targetAmount" to goal.targetAmount,
                "currentAmount" to goal.currentAmount,
                "createdAt" to goal.createdAt,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        } else {
            mapOf(
                "userId" to userId,
                "title" to goal.title.trim(),
                "targetAmount" to goal.targetAmount,
                "currentAmount" to goal.currentAmount,
                "createdAt" to FieldValue.serverTimestamp()
            )
        }

        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val op = if (isUpdate) OperationType.UPDATE else OperationType.CREATE
            handleFirestoreError(e, op, docRef.path)
            Result.failure(e)
        }
    }

    // Delete savings goal
    suspend fun deleteGoal(userId: String, goalId: String): Result<Unit> {
        require(goalId.isNotBlank()) { "Goal ID cannot be blank" }
        val docRef = db.collection("users").document(userId).collection("goals").document(goalId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }
}
