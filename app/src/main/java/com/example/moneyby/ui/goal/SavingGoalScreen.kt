@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.moneyby.ui.goal

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.moneyby.data.SavingGoal
import com.example.moneyby.data.Transaction
import com.example.moneyby.ui.AppViewModelProvider
import androidx.compose.ui.res.stringResource
import com.example.moneyby.R
import java.text.NumberFormat
import com.example.moneyby.util.formatCurrency
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingGoalScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SavingGoalViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.savingGoalsUiState.collectAsStateWithLifecycle()
    val accounts by viewModel.accountsState.collectAsStateWithLifecycle()
    var showGoalDialog by remember { mutableStateOf(false) }
    var goalToEdit by remember { mutableStateOf<SavingGoal?>(null) }
    val event by viewModel.event.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(event) {
        when (val e = event) {
            is SavingGoalEvent.Error -> {
                snackbarHostState.showSnackbar(e.message)
                viewModel.consumeEvent()
            }
            null -> Unit
        }
    }

    Scaffold(
        modifier = modifier.safeDrawingPadding(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.saving_goals_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { 
                    goalToEdit = null
                    showGoalDialog = true 
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add_goal)) }
            )
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = uiState.goals.isEmpty(),
            transitionSpec = {
                fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400))
            },
            label = "GoalsListTransition"
        ) { isEmpty ->
            if (isEmpty) {
                Box(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Savings, 
                            contentDescription = null, 
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.no_saving_goals_yet), 
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            stringResource(R.string.start_saving_today), 
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 80.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(uiState.goals, key = { it.id }) { goal ->
                            val history by viewModel.getGoalTransactions(goal.id).collectAsState(initial = emptyList())
                            SavingGoalItem(
                                goal = goal,
                                accounts = accounts,
                                history = history,
                                onEditClick = {
                                    goalToEdit = goal
                                    showGoalDialog = true
                                },
                                onDeleteClick = {
                                    viewModel.deleteGoal(goal)
                                },
                                onQuickAdd = { amount, type, accountId ->
                                    viewModel.addGoalTransaction(goal, amount, type, accountId)
                                },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }
        }

        if (showGoalDialog) {
            AddEditGoalDialog(
                goal = goalToEdit,
                onDismiss = { 
                    showGoalDialog = false
                    goalToEdit = null
                },
                onConfirm = { name, target, current, date ->
                    viewModel.saveGoal(name, target, current, date, goalToEdit?.id ?: 0)
                    showGoalDialog = false
                    goalToEdit = null
                }
            )
        }
    }
}

@Composable
fun SavingGoalItem(
    goal: SavingGoal,
    accounts: List<com.example.moneyby.data.Account>,
    history: List<com.example.moneyby.data.Transaction>,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onQuickAdd: (Double, String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = (goal.currentAmount / goal.targetAmount).coerceIn(0.0, 1.0).toFloat()
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "GoalProgress"
    )
    val isCompleted = goal.isCompleted || progress >= 1.0f
    
    var showQuickAddDialog by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    
    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp, 
            if (isCompleted) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) 
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (goal.targetDate > 0) {
                        Text(
                            text = stringResource(R.string.target_date_label, SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(goal.targetDate))),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row {
                    FilledIconButton(
                        onClick = onEditClick,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit), modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = onDeleteClick,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), modifier = Modifier.size(16.dp))
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = currencyFormatter.format(goal.currentAmount) + " saved",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.of_target_amount, currencyFormatter.format(goal.targetAmount)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!isCompleted) {
                    Button(
                        onClick = { showQuickAddDialog = true },
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        modifier = Modifier.height(36.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.update), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth().height(10.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                strokeCap = StrokeCap.Round
            )
            
            if (isCompleted) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    Text(
                        text = "🎉 " + stringResource(R.string.goal_achieved),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            
            TextButton(
                onClick = { showHistory = !showHistory },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(0.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(
                    imageVector = if (showHistory) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    if (showHistory) stringResource(R.string.hide_history) else stringResource(R.string.show_history), 
                    style = MaterialTheme.typography.labelMedium
                )
            }

            AnimatedVisibility(
                visible = showHistory,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    if (history.isEmpty()) {
                        Text(
                            stringResource(R.string.no_transactions_yet), 
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    } else {
                        Spacer(Modifier.height(8.dp))
                        history.take(5).forEach { transaction ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.History, 
                                        contentDescription = null, 
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(Date(transaction.date)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "${if (transaction.type == "Income") "+" else "-"}${currencyFormatter.format(transaction.amount)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (transaction.type == "Income") com.example.moneyby.ui.theme.IncomeGreen else com.example.moneyby.ui.theme.ExpenseRed
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    if (showQuickAddDialog) {
        QuickAddDialog(
            goal = goal,
            accounts = accounts,
            onDismiss = { showQuickAddDialog = false },
            onConfirm = { amount, type, accountId ->
                onQuickAdd(amount, type, accountId)
                showQuickAddDialog = false
            }
        )
    }
}

@Composable
fun QuickAddDialog(
    goal: SavingGoal,
    accounts: List<com.example.moneyby.data.Account>,
    onDismiss: () -> Unit,
    onConfirm: (Double, String, Int) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: 0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.update_progress)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.adding_money_to, goal.name), style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text(stringResource(R.string.amount)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    prefix = { Text("₹") }
                )
                
                Column {
                    Text(stringResource(R.string.select_funding_account), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    accounts.forEach { account ->
                        Surface(
                            onClick = { selectedAccountId = account.id },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            color = if (selectedAccountId == account.id) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            border = if (selectedAccountId == account.id) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedAccountId == account.id,
                                    onClick = { selectedAccountId = account.id }
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(account.name, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { 
                        val amt = amount.toDoubleOrNull() ?: 0.0
                        if (amt > 0 && selectedAccountId != 0) {
                            onConfirm(amt, "Expense", selectedAccountId)
                        }
                    },
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(stringResource(R.string.withdraw))
                }
                Button(
                    onClick = { 
                        val amt = amount.toDoubleOrNull() ?: 0.0
                        if (amt > 0 && selectedAccountId != 0) {
                            onConfirm(amt, "Income", selectedAccountId)
                        }
                    },
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(stringResource(R.string.deposit))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditGoalDialog(
    goal: SavingGoal? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Double, Long) -> Unit
) {
    var name by remember { mutableStateOf(goal?.name ?: "") }
    var targetAmount by remember { mutableStateOf(goal?.targetAmount?.toString() ?: "") }
    var currentAmount by remember { mutableStateOf(goal?.currentAmount?.toString() ?: "") }
    
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = if (goal?.targetDate != null && goal.targetDate > 0) goal.targetDate else System.currentTimeMillis())
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (goal == null) stringResource(R.string.new_saving_goal) else stringResource(R.string.edit_saving_goal)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.goal_name)) },
                    placeholder = { Text(stringResource(R.string.goal_name_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )
                OutlinedTextField(
                    value = targetAmount,
                    onValueChange = { targetAmount = it },
                    label = { Text(stringResource(R.string.target_amount)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    prefix = { Text("₹") }
                )
                OutlinedTextField(
                    value = currentAmount,
                    onValueChange = { currentAmount = it },
                    label = { Text(stringResource(R.string.initial_savings_optional)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    prefix = { Text("₹") }
                )
                
                OutlinedTextField(
                    value = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(datePickerState.selectedDateMillis ?: System.currentTimeMillis())),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.target_date)) },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = stringResource(R.string.select_date))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val target = targetAmount.toDoubleOrNull() ?: 0.0
                    val current = currentAmount.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && target > 0) {
                        onConfirm(name, target, current, datePickerState.selectedDateMillis ?: 0L)
                    }
                },
                shape = MaterialTheme.shapes.medium
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
