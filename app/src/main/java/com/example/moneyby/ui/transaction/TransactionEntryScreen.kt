@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.moneyby.ui.transaction

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.res.stringResource
import com.example.moneyby.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.moneyby.data.Account
import com.example.moneyby.ui.AppViewModelProvider
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TransactionEntryScreen(
    navigateBack: () -> Unit,
    transactionId: Int = 0,
    modifier: Modifier = Modifier,
    viewModel: TransactionEntryViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val coroutineScope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val accounts by viewModel.accountsState.collectAsStateWithLifecycle()
    val categories by viewModel.categoriesState.collectAsStateWithLifecycle()
    val event by viewModel.event.collectAsStateWithLifecycle()
    val isEditMode = transactionId != 0

    // Load existing transaction in edit mode
    LaunchedEffect(transactionId) {
        if (isEditMode) viewModel.loadTransaction(transactionId)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Handle one-shot events — navigate ONLY after form is reset
    LaunchedEffect(event) {
        when (val e = event) {
            is TransactionEvent.SaveSuccess -> {
                viewModel.consumeEvent()
                navigateBack()                          // form already reset in VM
            }
            is TransactionEvent.Error -> {
                viewModel.consumeEvent()
                snackbarHostState.showSnackbar(
                    e.message.ifBlank { context.getString(R.string.generic_error) }
                )
            }
            null -> Unit
        }
    }

    Scaffold(
        modifier = modifier.safeDrawingPadding(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEditMode) stringResource(R.string.edit_transaction)
                        else stringResource(R.string.add_transaction),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            TransactionEntryBody(
                transactionUiState = uiState,
                accounts = accounts,
                categories = categories,
                onTransactionValueChange = viewModel::updateUiState,
                onSaveClick = {
                    coroutineScope.launch {
                        if (isEditMode) viewModel.updateTransaction()
                        else viewModel.saveTransaction()
                    }
                },
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
            )
        }
    }
}

@Composable
fun TransactionEntryBody(
    transactionUiState: TransactionUiState,
    accounts: List<Account>,
    categories: List<com.example.moneyby.data.Category>,
    onTransactionValueChange: (TransactionDetails) -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            TransactionInputForm(
                transactionDetails = transactionUiState.transactionDetails,
                accounts = accounts,
                categories = categories,
                onValueChange = onTransactionValueChange,
                modifier = Modifier.padding(16.dp)
            )
        }

        Button(
            onClick = onSaveClick,
            enabled = transactionUiState.isEntryValid && !transactionUiState.isSaving,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            if (transactionUiState.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(Icons.Default.Done, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.save_transaction),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun TransactionInputForm(
    transactionDetails: TransactionDetails,
    accounts: List<Account>,
    categories: List<com.example.moneyby.data.Category>,
    modifier: Modifier = Modifier,
    onValueChange: (TransactionDetails) -> Unit = {},
    enabled: Boolean = true
) {
    val categoryNames = categories.filter { it.type == transactionDetails.type }.map { it.name }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Type Selection
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            listOf("Expense", "Income", "Transfer").forEachIndexed { index, type ->
                SegmentedButton(
                    selected = transactionDetails.type == type,
                    onClick = {
                        onValueChange(
                            transactionDetails.copy(
                                type = type,
                                category = if (type == "Transfer") "Transfer" else "",
                                toAccountId = 0
                            )
                        )
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = 3)
                ) {
                    Text(
                        when (type) {
                            "Expense" -> stringResource(R.string.expense)
                            "Income"  -> stringResource(R.string.income)
                            else      -> stringResource(R.string.transfer)
                        }
                    )
                }
            }
        }

        // Amount
        OutlinedTextField(
            value = transactionDetails.amount,
            onValueChange = { onValueChange(transactionDetails.copy(amount = it)) },
            label = { Text(stringResource(R.string.amount)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            singleLine = true,
            leadingIcon = {
                Text(
                    stringResource(R.string.currency_symbol),
                    modifier = Modifier.padding(start = 12.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )

        // From Account
        var accountExpanded by remember { mutableStateOf(false) }
        val selectedAccount = accounts.find { it.id == transactionDetails.accountId }
        ExposedDropdownMenuBox(
            expanded = accountExpanded,
            onExpandedChange = { accountExpanded = !accountExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedAccount?.name ?: stringResource(R.string.select_account),
                onValueChange = {},
                readOnly = true,
                label = {
                    Text(
                        if (transactionDetails.type == "Transfer") stringResource(R.string.from_account)
                        else stringResource(R.string.account)
                    )
                },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded) },
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                    .fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) }
            )
            ExposedDropdownMenu(expanded = accountExpanded, onDismissRequest = { accountExpanded = false }) {
                accounts.forEach { account ->
                    DropdownMenuItem(
                        text = { Text(account.name) },
                        onClick = {
                            onValueChange(transactionDetails.copy(accountId = account.id))
                            accountExpanded = false
                        }
                    )
                }
            }
        }

        // To Account (Transfer only)
        if (transactionDetails.type == "Transfer") {
            var toAccountExpanded by remember { mutableStateOf(false) }
            val selectedToAccount = accounts.find { it.id == transactionDetails.toAccountId }
            ExposedDropdownMenuBox(
                expanded = toAccountExpanded,
                onExpandedChange = { toAccountExpanded = !toAccountExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedToAccount?.name ?: stringResource(R.string.select_destination_account),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.to_account)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toAccountExpanded) },
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                        .fillMaxWidth(),
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) }
                )
                ExposedDropdownMenu(expanded = toAccountExpanded, onDismissRequest = { toAccountExpanded = false }) {
                    accounts.filter { it.id != transactionDetails.accountId }.forEach { account ->
                        DropdownMenuItem(
                            text = { Text(account.name) },
                            onClick = {
                                onValueChange(transactionDetails.copy(toAccountId = account.id))
                                toAccountExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Category (non-Transfer only)
        if (transactionDetails.type != "Transfer") {
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = transactionDetails.category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.category)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                        .fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) }
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    if (categoryNames.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.no_categories_found)) },
                            onClick = {},
                            enabled = false
                        )
                    } else {
                        categoryNames.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    onValueChange(transactionDetails.copy(category = category))
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Date picker
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = transactionDetails.date)
        var showDatePicker by remember { mutableStateOf(false) }

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        onValueChange(
                            transactionDetails.copy(
                                date = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                            )
                        )
                        showDatePicker = false
                    }) { Text(stringResource(R.string.ok)) }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) }
                }
            ) { DatePicker(state = datePickerState) }
        }

        OutlinedTextField(
            value = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(transactionDetails.date)),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.date)) },
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.CalendarToday, contentDescription = stringResource(R.string.select_date))
                }
            },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) }
        )

        // Notes
        OutlinedTextField(
            value = transactionDetails.notes,
            onValueChange = { onValueChange(transactionDetails.copy(notes = it)) },
            label = { Text(stringResource(R.string.notes_optional)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null) }
        )
    }
}
