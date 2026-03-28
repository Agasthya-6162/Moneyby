@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.moneyby.ui.recurring

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.moneyby.data.RecurringTransaction
import com.example.moneyby.ui.AppViewModelProvider
import androidx.compose.ui.res.stringResource
import com.example.moneyby.R
import com.example.moneyby.util.formatCurrency
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RecurringTransactionScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecurringTransactionViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val event by viewModel.event.collectAsStateWithLifecycle()
    val hostState = remember { SnackbarHostState() }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val showDialog = remember { mutableStateOf(false) }

    LaunchedEffect(event) {
        when (val e = event) {
            is RecurringEvent.Error -> {
                hostState.showSnackbar(e.message)
                viewModel.consumeEvent()
            }
            null -> Unit
        }
    }

    Scaffold(
        modifier = modifier.safeDrawingPadding(),
        snackbarHost = { SnackbarHost(hostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.recurring_transactions_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog.value = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_recurring))
            }
        }
    ) { innerPadding ->
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
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.transactions) { transaction ->
                    RecurringTransactionItem(
                        transaction = transaction,
                        onDelete = { viewModel.deleteRecurringTransaction(transaction) }
                    )
                }
            }
        }

        if (showDialog.value) {
            AddRecurringTransactionDialog(
                accounts = accounts,
                onDismiss = { showDialog.value = false },
                onConfirm = { name, amount, category, type, accountId, frequency, nextRun ->
                    viewModel.saveRecurringTransaction(name, amount, category, type, accountId, frequency, nextRun)
                    showDialog.value = false
                }
            )
        }
    }
}

@Composable
fun RecurringTransactionItem(
    transaction: RecurringTransaction,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(transaction.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "${transaction.frequency} • ${formatCurrency(transaction.amount)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Next: ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(transaction.nextRunDate))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun AddRecurringTransactionDialog(
    accounts: List<com.example.moneyby.data.Account>,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String, String, Int, String, Long) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Rent") }
    var type by remember { mutableStateOf("Expense") }
    var selectedAccountId by remember { mutableIntStateOf(accounts.firstOrNull()?.id ?: 0) }
    var frequency by remember { mutableStateOf("Monthly") }
    var selectedDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val showDatePicker = remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate
    )

    if (showDatePicker.value) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker.value = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDate = datePickerState.selectedDateMillis
                        ?: System.currentTimeMillis()
                    showDatePicker.value = false
                }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker.value = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_recurring_transaction)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.template_name_label)) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text(stringResource(R.string.amount)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    "Start Date",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                OutlinedButton(
                    onClick = { showDatePicker.value = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                            .format(Date(selectedDate))
                    )
                }

                Text(
                    stringResource(R.string.select_account),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                if (accounts.isEmpty()) {
                    Text(
                        stringResource(R.string.no_accounts_found),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Column {
                        accounts.forEach { account ->
                            Surface(
                                onClick = { selectedAccountId = account.id },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.small,
                                color = if (selectedAccountId == account.id)
                                    MaterialTheme.colorScheme.primaryContainer
                                else Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedAccountId == account.id,
                                        onClick = { selectedAccountId = account.id }
                                    )
                                    Text(account.name, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }

                Text(
                    stringResource(R.string.frequency_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                val frequencies = mapOf(
                    "Daily" to stringResource(R.string.daily),
                    "Weekly" to stringResource(R.string.weekly),
                    "Monthly" to stringResource(R.string.monthly)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    frequencies.forEach { (key, label) ->
                        FilterChip(
                            selected = frequency == key,
                            onClick = { frequency = key },
                            label = { Text(label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && amt > 0 && selectedAccountId != 0) {
                        onConfirm(name, amt, category, type, selectedAccountId, frequency, selectedDate)
                    }
                },
                enabled = name.isNotBlank() && (amount.toDoubleOrNull() ?: 0.0) > 0 && selectedAccountId != 0
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
