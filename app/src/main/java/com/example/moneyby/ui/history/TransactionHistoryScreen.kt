package com.example.moneyby.ui.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.moneyby.ui.theme.IncomeGreen
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.moneyby.data.Account
import com.example.moneyby.data.Transaction
import com.example.moneyby.ui.AppViewModelProvider
import androidx.compose.ui.res.stringResource
import com.example.moneyby.R
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TransactionHistoryScreen(
    onBackClick: () -> Unit,
    onEditClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TransactionHistoryViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.historyUiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedAccountId by viewModel.selectedAccountId.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val dateRange by viewModel.dateRange.collectAsStateWithLifecycle()

    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.safeDrawingPadding(),
        topBar = {
            TopAppBar(
                title = { 
                    Text(stringResource(R.string.transaction_history), style = MaterialTheme.typography.titleLarge)
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = stringResource(R.string.filter_by_date))
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text(stringResource(R.string.search_placeholder)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = MaterialTheme.shapes.medium,
                singleLine = true
            )

            // Account Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedAccountId == null,
                        onClick = { viewModel.selectAccount(null) },
                        label = { Text(stringResource(R.string.all_accounts)) }
                    )
                }
                items(accounts) { account ->
                    FilterChip(
                        selected = selectedAccountId == account.id,
                        onClick = { viewModel.selectAccount(account.id) },
                        label = { Text(account.name) }
                    )
                }
            }

            if (dateRange.first != null || dateRange.second != null) {
                val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val start = dateRange.first?.let { sdf.format(Date(it)) } ?: "..."
                val end = dateRange.second?.let { sdf.format(Date(it)) } ?: "..."
                AssistChip(
                    onClick = { viewModel.updateDateRange(null, null) },
                    label = { Text(stringResource(R.string.date_range_label, start, end)) },
                    modifier = Modifier.padding(horizontal = 16.dp),
                    trailingIcon = { Icon(Icons.Default.FilterList, contentDescription = stringResource(R.string.clear)) }
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                uiState.groupedTransactions.forEach { (date, transactions) ->
                    stickyHeader {
                        Text(
                            text = date,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(transactions) { transaction ->
                        TransactionHistoryItem(
                            transaction = transaction,
                            accounts = accounts,
                            onEditClick = { onEditClick(transaction.id) }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
        }

        if (showDatePicker) {
            val dateRangePickerState = rememberDateRangePickerState()
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.updateDateRange(
                            dateRangePickerState.selectedStartDateMillis,
                            dateRangePickerState.selectedEndDateMillis
                        )
                        showDatePicker = false
                    }) {
                        Text(stringResource(R.string.ok))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            ) {
                DateRangePicker(
                    state = dateRangePickerState,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryItem(
    transaction: Transaction,
    accounts: List<Account>,
    onEditClick: () -> Unit
) {
    val accountName = accounts.find { it.id == transaction.accountId }?.name ?: "Unknown"
    val categoryIcon = when (transaction.category) {
        "Food" -> Icons.Default.Restaurant
        "Transport" -> Icons.Default.DirectionsCar
        "Shopping" -> Icons.Default.ShoppingBag
        "Entertainment" -> Icons.Default.Movie
        "Health" -> Icons.Default.HealthAndSafety
        "Salary" -> Icons.Default.Payments
        "Investment" -> Icons.AutoMirrored.Filled.TrendingUp
        "Gift" -> Icons.Default.CardGiftcard
        else -> if (transaction.type == "Expense") Icons.Default.ArrowDownward else Icons.Default.ArrowUpward
    }

    val dismissState = rememberSwipeToDismissBoxState()
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onEditClick()
            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    ) {
        ListItem(
            headlineContent = { Text(transaction.category, fontWeight = FontWeight.Medium) },
            supportingContent = { 
                Column {
                    Text(
                        "$accountName • ${transaction.type}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (transaction.type == "Expense") MaterialTheme.colorScheme.error else IncomeGreen
                    )
                    if (transaction.notes.isNotBlank()) {
                        Text(transaction.notes, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            trailingContent = {
                Text(
                    (if (transaction.type == "Expense") "-" else "+") +
                    NumberFormat.getCurrencyInstance(java.util.Locale.forLanguageTag("en-IN")).format(transaction.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (transaction.type == "Expense")
                        MaterialTheme.colorScheme.error else IncomeGreen
                )
            },
            leadingContent = {
                Icon(
                    categoryIcon,
                    contentDescription = null,
                    tint = if (transaction.type == "Expense") MaterialTheme.colorScheme.error else IncomeGreen
                )
            }
        )
    }
}
