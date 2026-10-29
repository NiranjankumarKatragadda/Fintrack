package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Budget
import com.example.data.model.BudgetAlertSeverity
import com.example.data.model.CategoryBudgetAlert
import com.example.data.model.Expense
import com.example.data.model.SavingsGoal
import com.example.data.repository.FinanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "FinanceVM"
private const val STOP_TIMEOUT_MILLIS = 5000L

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class FinancialSummary(
    val totalBalance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val savingsRate: Int = 0
)

class FinanceViewModel(
    private val repository: FinanceRepository,
    private val currentUserId: String
) : ViewModel() {

    // UI Message / Toast state
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage = _userMessage.asStateFlow()

    fun clearUserMessage() {
        _userMessage.value = null
    }

    // 1. Observe real-time Expenses
    val expensesState: StateFlow<UiState<List<Expense>>> = repository.observeExpenses(currentUserId)
        .map<List<Expense>, UiState<List<Expense>>> { list ->
            // Sort by createdAt descending or date descending
            val sorted = list.sortedByDescending { it.createdAt?.seconds ?: 0L }
            UiState.Success(sorted)
        }
        .catch { error ->
            Log.w(TAG, "Error observing expenses", error)
            emit(UiState.Error(error.localizedMessage ?: "Failed to load expenses"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    // 2. Observe real-time Budgets
    val budgetsState: StateFlow<UiState<List<Budget>>> = repository.observeBudgets(currentUserId)
        .map<List<Budget>, UiState<List<Budget>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing budgets", error)
            emit(UiState.Error(error.localizedMessage ?: "Failed to load budgets"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    // 3. Observe real-time Savings Goals
    val goalsState: StateFlow<UiState<List<SavingsGoal>>> = repository.observeGoals(currentUserId)
        .map<List<SavingsGoal>, UiState<List<SavingsGoal>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing goals", error)
            emit(UiState.Error(error.localizedMessage ?: "Failed to load savings goals"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    // 4. Combined Budget Alerts: Calculates spent per category and computes alerts
    val budgetAlerts: StateFlow<List<CategoryBudgetAlert>> = combine(
        repository.observeExpenses(currentUserId),
        repository.observeBudgets(currentUserId)
    ) { expenses, budgets ->
        if (budgets.isEmpty()) return@combine emptyList()

        // Sum expenses by category
        val categoryExpenses = mutableMapOf<String, Double>()
        for (expense in expenses) {
            if (expense.type.equals("EXPENSE", ignoreCase = true)) {
                val current = categoryExpenses.getOrDefault(expense.category, 0.0)
                categoryExpenses[expense.category] = current + expense.amount
            }
        }

        budgets.map { budget ->
            val spent = categoryExpenses.getOrDefault(budget.category, 0.0)
            val limit = budget.monthlyLimit
            val percentage = if (limit > 0) ((spent / limit) * 100).toInt() else 0

            val severity = when {
                percentage >= 100 -> BudgetAlertSeverity.EXCEEDED
                percentage >= budget.alertThresholdPercent -> BudgetAlertSeverity.WARNING
                else -> BudgetAlertSeverity.SAFE
            }

            val message = when (severity) {
                BudgetAlertSeverity.EXCEEDED ->
                    "Over budget! Spent $${"%.2f".format(spent)} of $${"%.2f".format(limit)} ($percentage%)"
                BudgetAlertSeverity.WARNING ->
                    "Approaching limit! Reached $percentage% ($${"%.2f".format(spent)} / $${"%.2f".format(limit)})"
                BudgetAlertSeverity.SAFE ->
                    "On track: $${"%.2f".format(spent)} spent of $${"%.2f".format(limit)}"
            }

            CategoryBudgetAlert(
                category = budget.category,
                spent = spent,
                limit = limit,
                percentage = percentage,
                thresholdPercent = budget.alertThresholdPercent,
                severity = severity,
                alertMessage = message
            )
        }.sortedByDescending { it.percentage }
    }.catch { error ->
        Log.w(TAG, "Error computing budget alerts", error)
        emit(emptyList())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptyList()
    )

    // 5. Financial Summary (Total Balance, Income, Expenses)
    val financialSummary: StateFlow<FinancialSummary> = repository.observeExpenses(currentUserId)
        .map { expenses ->
            var income = 0.0
            var expense = 0.0
            for (item in expenses) {
                if (item.type.equals("INCOME", ignoreCase = true)) {
                    income += item.amount
                } else {
                    expense += item.amount
                }
            }
            val balance = income - expense
            val savingsRate = if (income > 0) (((income - expense) / income) * 100).coerceAtLeast(0.0).toInt() else 0
            FinancialSummary(
                totalBalance = balance,
                totalIncome = income,
                totalExpense = expense,
                savingsRate = savingsRate
            )
        }
        .catch {
            emit(FinancialSummary())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = FinancialSummary()
        )

    // Operations
    fun addExpense(
        title: String,
        amount: Double,
        type: String,
        category: String,
        date: String,
        note: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val expense = Expense(
                userId = currentUserId,
                title = title,
                amount = amount,
                type = type,
                category = category,
                date = date,
                note = note
            )
            val result = repository.addExpense(currentUserId, expense)
            if (result.isSuccess) {
                _userMessage.value = "Transaction added successfully!"
                onComplete(true)
            } else {
                _userMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to add transaction"
                onComplete(false)
            }
        }
    }

    fun updateExpense(
        expense: Expense,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = repository.updateExpense(currentUserId, expense)
            if (result.isSuccess) {
                _userMessage.value = "Transaction updated!"
                onComplete(true)
            } else {
                _userMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to update transaction"
                onComplete(false)
            }
        }
    }

    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            val result = repository.deleteExpense(currentUserId, expenseId)
            if (result.isSuccess) {
                _userMessage.value = "Transaction deleted"
            } else {
                _userMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to delete"
            }
        }
    }

    fun saveBudget(
        id: String = "",
        category: String,
        monthlyLimit: Double,
        alertThresholdPercent: Int,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val budget = Budget(
                id = id,
                userId = currentUserId,
                category = category,
                monthlyLimit = monthlyLimit,
                alertThresholdPercent = alertThresholdPercent
            )
            val result = repository.saveBudget(currentUserId, budget)
            if (result.isSuccess) {
                _userMessage.value = "Budget limit saved!"
                onComplete(true)
            } else {
                _userMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to save budget"
                onComplete(false)
            }
        }
    }

    fun deleteBudget(budgetId: String) {
        viewModelScope.launch {
            val result = repository.deleteBudget(currentUserId, budgetId)
            if (result.isSuccess) {
                _userMessage.value = "Budget removed"
            } else {
                _userMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to delete budget"
            }
        }
    }

    fun saveGoal(
        id: String = "",
        title: String,
        targetAmount: Double,
        currentAmount: Double,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val goal = SavingsGoal(
                id = id,
                userId = currentUserId,
                title = title,
                targetAmount = targetAmount,
                currentAmount = currentAmount
            )
            val result = repository.saveGoal(currentUserId, goal)
            if (result.isSuccess) {
                _userMessage.value = "Savings goal saved!"
                onComplete(true)
            } else {
                _userMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to save goal"
                onComplete(false)
            }
        }
    }

    fun addFundsToGoal(goal: SavingsGoal, additionalAmount: Double, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val updated = goal.copy(
                currentAmount = goal.currentAmount + additionalAmount
            )
            val result = repository.saveGoal(currentUserId, updated)
            if (result.isSuccess) {
                _userMessage.value = "+$${"%.2f".format(additionalAmount)} added to ${goal.title}!"
                onComplete()
            } else {
                _userMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to update savings"
            }
        }
    }

    fun deleteGoal(goalId: String) {
        viewModelScope.launch {
            val result = repository.deleteGoal(currentUserId, goalId)
            if (result.isSuccess) {
                _userMessage.value = "Goal deleted"
            } else {
                _userMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to delete goal"
            }
        }
    }
}
