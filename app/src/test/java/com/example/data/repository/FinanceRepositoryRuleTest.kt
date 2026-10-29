package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.Budget
import com.example.data.model.Expense
import com.example.data.model.SavingsGoal
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FinanceRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun addExpense_authenticatedUser_success() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repository = FinanceRepository(firestore)

    val expense = Expense(
      title = "Groceries",
      amount = 75.50,
      type = "EXPENSE",
      category = "Food & Dining",
      date = "2026-10-29",
      note = "Supermarket"
    )

    val result = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.addExpense(uid, expense)
    }

    assertTrue(result.isSuccess)
    val id = result.getOrThrow()
    assertTrue(id.isNotEmpty())

    val expenses = withTimeout(FLOW_TIMEOUT_MS) {
      repository.observeExpenses(uid).first { list: List<Expense> -> list.any { it.id == id } }
    }
    assertTrue(expenses.map { it.id }.contains(id))
  }

  @Test
  fun saveBudget_authenticatedUser_success() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repository = FinanceRepository(firestore)

    val budget = Budget(
      category = "Food & Dining",
      monthlyLimit = 500.0,
      alertThresholdPercent = 80
    )

    val result = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.saveBudget(uid, budget)
    }

    assertTrue(result.isSuccess)
    val id = result.getOrThrow()

    val budgets = withTimeout(FLOW_TIMEOUT_MS) {
      repository.observeBudgets(uid).first { list: List<Budget> -> list.any { it.id == id } }
    }
    assertTrue(budgets.any { it.category == "Food & Dining" && it.monthlyLimit == 500.0 })
  }

  @Test
  fun saveGoal_authenticatedUser_success() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repository = FinanceRepository(firestore)

    val goal = SavingsGoal(
      title = "Vacation Fund",
      targetAmount = 2000.0,
      currentAmount = 450.0
    )

    val result = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.saveGoal(uid, goal)
    }

    assertTrue(result.isSuccess)
    val id = result.getOrThrow()

    val goals = withTimeout(FLOW_TIMEOUT_MS) {
      repository.observeGoals(uid).first { list: List<SavingsGoal> -> list.any { it.id == id } }
    }
    assertTrue(goals.any { it.title == "Vacation Fund" })
  }

  @Test
  fun observeExpenses_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repository = FinanceRepository(firestore)

    val exception = assertThrows(Exception::class.java) {
      runBlocking {
        withTimeout(FLOW_TIMEOUT_MS) {
          repository.observeExpenses("unauthenticated_uid").first()
        }
      }
    }

    val firestoreException = checkNotNull(exception.unwrapFirestoreException()) {
      "Expected nested FirebaseFirestoreException, got $exception"
    }
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
  }

  private companion object {
    const val ALICE_EMAIL = "alice_rules@test.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 4000L
  }
}
