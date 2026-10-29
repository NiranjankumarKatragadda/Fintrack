package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Budget
import com.example.data.model.BudgetAlertSeverity
import com.example.data.model.CategoryBudgetAlert
import com.example.ui.components.AddBudgetDialog
import com.example.ui.components.getCategoryIcon
import com.example.ui.theme.AlertAmber
import com.example.ui.theme.AlertAmberContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ExpenseRedContainer
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.IncomeGreenContainer
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.viewmodel.UiState

@Composable
fun BudgetsScreen(viewModel: FinanceViewModel) {
    val budgetsState by viewModel.budgetsState.collectAsState()
    val alerts by viewModel.budgetAlerts.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingBudget by remember { mutableStateOf<Budget?>(null) }
    var deletingBudget by remember { mutableStateOf<Budget?>(null) }

    val totalBudgeted = alerts.sumOf { it.limit }
    val totalSpent = alerts.sumOf { it.spent }
    val overallPercentage = if (totalBudgeted > 0) ((totalSpent / totalBudgeted) * 100).toInt() else 0

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp)
        ) {
            // Overall Budget Summary Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF0A2540))
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Monthly Spending vs Budget",
                                        fontSize = 13.sp,
                                        color = Color(0xFFCBD5E1)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "$${"%,.2f".format(totalSpent)} / $${"%,.2f".format(totalBudgeted)}",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (overallPercentage >= 100) ExpenseRed else EmeraldPrimary
                                ) {
                                    Text(
                                        text = "$overallPercentage%",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            LinearProgressIndicator(
                                progress = { (overallPercentage / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = if (overallPercentage >= 100) ExpenseRed else EmeraldPrimary,
                                trackColor = Color(0xFF334155),
                                strokeCap = StrokeCap.Round
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = if (totalBudgeted > 0) {
                                    if (totalSpent > totalBudgeted) {
                                        "⚠️ Overall spending exceeded total monthly budget!"
                                    } else {
                                        "Remaining this month: $${"%,.2f".format(totalBudgeted - totalSpent)}"
                                    }
                                } else {
                                    "No category budgets configured yet"
                                },
                                fontSize = 12.sp,
                                color = if (totalSpent > totalBudgeted) ExpenseRed else Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Category Budgets & Alert Thresholds",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            when (val state = budgetsState) {
                is UiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = EmeraldPrimary)
                        }
                    }
                }
                is UiState.Error -> {
                    item {
                        Text(text = state.message, color = ExpenseRed, modifier = Modifier.padding(16.dp))
                    }
                }
                is UiState.Success -> {
                    if (state.data.isEmpty()) {
                        item {
                            EmptyBudgetsCard(onAdd = { showAddDialog = true })
                        }
                    } else {
                        items(state.data, key = { it.id }) { budget ->
                            val alert = alerts.find { it.category == budget.category }
                            BudgetDetailCard(
                                budget = budget,
                                alert = alert,
                                onEdit = { editingBudget = budget },
                                onDelete = { deletingBudget = budget }
                            )
                        }
                    }
                }
            }
        }

        // FAB to Add Budget
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = EmeraldPrimary,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp)
                .testTag("add_budget_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Set Budget")
        }

        // Add Dialog
        if (showAddDialog) {
            AddBudgetDialog(
                onDismiss = { showAddDialog = false },
                onSave = { category, limit, threshold ->
                    viewModel.saveBudget(
                        category = category,
                        monthlyLimit = limit,
                        alertThresholdPercent = threshold
                    )
                    showAddDialog = false
                }
            )
        }

        // Edit Dialog
        editingBudget?.let { budget ->
            AddBudgetDialog(
                initialBudget = budget,
                onDismiss = { editingBudget = null },
                onSave = { category, limit, threshold ->
                    viewModel.saveBudget(
                        id = budget.id,
                        category = category,
                        monthlyLimit = limit,
                        alertThresholdPercent = threshold
                    )
                    editingBudget = null
                }
            )
        }

        // Delete Dialog
        deletingBudget?.let { budget ->
            AlertDialog(
                onDismissRequest = { deletingBudget = null },
                title = { Text("Delete Budget?") },
                text = { Text("Remove the monthly budget for \"${budget.category}\"?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteBudget(budget.id)
                            deletingBudget = null
                        }
                    ) {
                        Text("Delete", color = ExpenseRed, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deletingBudget = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun BudgetDetailCard(
    budget: Budget,
    alert: CategoryBudgetAlert?,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val spent = alert?.spent ?: 0.0
    val limit = budget.monthlyLimit
    val percentage = if (limit > 0) ((spent / limit) * 100).toInt() else 0
    val severity = alert?.severity ?: BudgetAlertSeverity.SAFE

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(EmeraldPrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getCategoryIcon(budget.category),
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = budget.category,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = AlertAmber,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Alert at ${budget.alertThresholdPercent}%",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (severity) {
                        BudgetAlertSeverity.EXCEEDED -> ExpenseRedContainer
                        BudgetAlertSeverity.WARNING -> AlertAmberContainer
                        BudgetAlertSeverity.SAFE -> IncomeGreenContainer
                    }
                ) {
                    Text(
                        text = "$percentage%",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = when (severity) {
                            BudgetAlertSeverity.EXCEEDED -> ExpenseRed
                            BudgetAlertSeverity.WARNING -> AlertAmber
                            BudgetAlertSeverity.SAFE -> IncomeGreen
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { (percentage / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = when (severity) {
                    BudgetAlertSeverity.EXCEEDED -> ExpenseRed
                    BudgetAlertSeverity.WARNING -> AlertAmber
                    BudgetAlertSeverity.SAFE -> EmeraldPrimary
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$${"%.2f".format(spent)} spent of $${"%.2f".format(limit)}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Budget", modifier = Modifier.size(17.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Budget", modifier = Modifier.size(17.dp), tint = ExpenseRed)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyBudgetsCard(onAdd: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.PieChart,
                contentDescription = null,
                tint = EmeraldPrimary,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No Budgets Configured",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Set monthly spending limits for categories like Food, Utilities, or Shopping. FinTrack will alert you in real-time when approaching limits.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(14.dp))
            TextButton(onClick = onAdd) {
                Text("+ Set First Category Budget", color = EmeraldPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}
