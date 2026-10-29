package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.example.data.model.SavingsGoal
import com.example.ui.components.AddFundsDialog
import com.example.ui.components.AddGoalDialog
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.IncomeGreenContainer
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.viewmodel.UiState

@Composable
fun GoalsScreen(viewModel: FinanceViewModel) {
    val goalsState by viewModel.goalsState.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<SavingsGoal?>(null) }
    var depositingGoal by remember { mutableStateOf<SavingsGoal?>(null) }
    var deletingGoal by remember { mutableStateOf<SavingsGoal?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp)
        ) {
            when (val state = goalsState) {
                is UiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
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
                    val goals = state.data
                    val totalSaved = goals.sumOf { it.currentAmount }
                    val totalTarget = goals.sumOf { it.targetAmount }
                    val overallProgress = if (totalTarget > 0) ((totalSaved / totalTarget) * 100).toInt() else 0

                    // Overview Card
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
                                            listOf(Color(0xFF0F172A), Color(0xFF134E4A), Color(0xFF0D9488))
                                        )
                                    )
                                    .padding(20.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "Total Savings Accumulated",
                                        fontSize = 13.sp,
                                        color = Color(0xFFCCFBF1)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "$${"%,.2f".format(totalSaved)}",
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "of $${"%,.2f".format(totalTarget)} target ($overallProgress% achieved)",
                                        fontSize = 13.sp,
                                        color = Color(0xFFCBD5E1)
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    LinearProgressIndicator(
                                        progress = { (overallProgress / 100f).coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = Color(0xFF2DD4BF),
                                        trackColor = Color(0xFF1E293B),
                                        strokeCap = StrokeCap.Round
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Active Savings Goals (${goals.size})",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }

                    if (goals.isEmpty()) {
                        item {
                            EmptyGoalsCard(onAdd = { showAddDialog = true })
                        }
                    } else {
                        items(goals, key = { it.id }) { goal ->
                            GoalItemCard(
                                goal = goal,
                                onAddFunds = { depositingGoal = goal },
                                onEdit = { editingGoal = goal },
                                onDelete = { deletingGoal = goal }
                            )
                        }
                    }
                }
            }
        }

        // FAB to Add Goal
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = EmeraldPrimary,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp)
                .testTag("add_goal_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Goal")
        }

        if (showAddDialog) {
            AddGoalDialog(
                onDismiss = { showAddDialog = false },
                onSave = { title, target, current ->
                    viewModel.saveGoal(title = title, targetAmount = target, currentAmount = current)
                    showAddDialog = false
                }
            )
        }

        editingGoal?.let { goal ->
            AddGoalDialog(
                initialGoal = goal,
                onDismiss = { editingGoal = null },
                onSave = { title, target, current ->
                    viewModel.saveGoal(id = goal.id, title = title, targetAmount = target, currentAmount = current)
                    editingGoal = null
                }
            )
        }

        depositingGoal?.let { goal ->
            AddFundsDialog(
                goal = goal,
                onDismiss = { depositingGoal = null },
                onAdd = { amount ->
                    viewModel.addFundsToGoal(goal, amount)
                    depositingGoal = null
                }
            )
        }

        deletingGoal?.let { goal ->
            AlertDialog(
                onDismissRequest = { deletingGoal = null },
                title = { Text("Delete Savings Goal?") },
                text = { Text("Delete \"${goal.title}\"?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteGoal(goal.id)
                            deletingGoal = null
                        }
                    ) {
                        Text("Delete", color = ExpenseRed, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deletingGoal = null }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
fun GoalItemCard(
    goal: SavingsGoal,
    onAddFunds: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val progress = if (goal.targetAmount > 0) {
        ((goal.currentAmount / goal.targetAmount) * 100).toInt()
    } else 0
    val isCompleted = progress >= 100

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
                            .background(
                                if (isCompleted) IncomeGreen.copy(alpha = 0.15f) else EmeraldPrimary.copy(alpha = 0.12f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            tint = if (isCompleted) IncomeGreen else EmeraldPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = goal.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$${"%,.2f".format(goal.currentAmount)} of $${"%,.2f".format(goal.targetAmount)}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isCompleted) IncomeGreenContainer else EmeraldPrimary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = if (isCompleted) "Completed! 🎉" else "$progress%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCompleted) IncomeGreen else EmeraldPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { (progress / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (isCompleted) IncomeGreen else EmeraldPrimary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onAddFunds,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary.copy(alpha = 0.12f),
                        contentColor = EmeraldPrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("+ Deposit", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(17.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(17.dp), tint = ExpenseRed)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyGoalsCard(onAdd: () -> Unit) {
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
                imageVector = Icons.Default.Savings,
                contentDescription = null,
                tint = EmeraldPrimary,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No Savings Goals Yet",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Set a financial milestone like an Emergency Fund, Vacation, or New Device, and track your progress as you save.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(14.dp))
            TextButton(onClick = onAdd) {
                Text("+ Create First Savings Goal", color = EmeraldPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}
