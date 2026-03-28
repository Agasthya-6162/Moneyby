@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)

package com.example.moneyby.ui.transaction

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.moneyby.R
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
                navigateBack()
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

    // Dynamic color logic based on selected type
    val targetColor = when (uiState.transactionDetails.type) {
        "Expense" -> Color(0xFFE53935) // Deep Red
        "Income" -> Color(0xFF4CAF50)  // Green
        "Transfer" -> Color(0xFF1E88E5) // Blue
        else -> MaterialTheme.colorScheme.primary
    }
    val animatedColor by animateColorAsState(targetValue = targetColor, animationSpec = tween(500), label = "HeaderColor")

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (uiState.isEntryValid) {
                ExtendedFloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            if (isEditMode) viewModel.updateTransaction()
                            else viewModel.saveTransaction()
                        }
                    },
                    containerColor = animatedColor,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.Check, contentDescription = null) },
                    text = { 
                        Text(
                            stringResource(R.string.save_transaction),
                            fontWeight = FontWeight.Bold
                        ) 
                    }
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Hero Header Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(
                        colors = listOf(animatedColor.copy(alpha = 0.9f), animatedColor.copy(alpha = 0.6f))
                    ))
                    .statusBarsPadding()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = innerPadding.calculateTopPadding() / 2),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top App Bar Elements
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = navigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = Color.White)
                        }
                        Text(
                            text = if (isEditMode) stringResource(R.string.edit_transaction) else stringResource(R.string.add_transaction),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.width(48.dp)) // Balance the back button
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = "How much?",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    val focusManager = LocalFocusManager.current
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 48.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.currency_symbol),
                            style = TextStyle(
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Light,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        BasicTextField(
                            value = uiState.transactionDetails.amount,
                            onValueChange = { viewModel.updateUiState(uiState.transactionDetails.copy(amount = it)) },
                            textStyle = TextStyle(
                                fontSize = 56.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Left
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            singleLine = true,
                            cursorBrush = SolidColor(Color.White),
                            modifier = Modifier.width(IntrinsicSize.Min).defaultMinSize(minWidth = 100.dp)
                        )
                    }
                }
            }

            // Input Form Content
            TransactionInputForm(
                transactionDetails = uiState.transactionDetails,
                accounts = accounts,
                categories = categories,
                animatedColor = animatedColor,
                onValueChange = viewModel::updateUiState,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            )
        }
    }
}

@Composable
fun TransactionInputForm(
    transactionDetails: TransactionDetails,
    accounts: List<Account>,
    categories: List<com.example.moneyby.data.Category>,
    animatedColor: Color,
    modifier: Modifier = Modifier,
    onValueChange: (TransactionDetails) -> Unit = {}
) {
    val categoryNames = categories.filter { it.type == transactionDetails.type }.map { it.name }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Custom Segmented Switcher for Type
        TypeSegmentedControl(
            selectedType = transactionDetails.type,
            activeColor = animatedColor,
            onTypeSelected = { type ->
                onValueChange(
                    transactionDetails.copy(
                        type = type,
                        category = if (type == "Transfer") "Transfer" else "",
                        toAccountId = 0
                    )
                )
            }
        )

        // Fields Container
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // From Account Selector
            val fromAccount = accounts.find { it.id == transactionDetails.accountId }
            val fromLabel = if (transactionDetails.type == "Transfer") stringResource(R.string.from_account) else stringResource(R.string.account)
            
            SelectionPopupCard(
                label = fromLabel,
                selectedValue = fromAccount?.name ?: stringResource(R.string.select_account),
                icon = Icons.Default.AccountBalanceWallet,
                items = accounts.map { it.name },
                onItemSelected = { selectedName ->
                    val acc = accounts.find { it.name == selectedName }
                    if (acc != null) {
                        onValueChange(transactionDetails.copy(accountId = acc.id))
                    }
                }
            )

            // Animated Transfer To Account Selector
            AnimatedVisibility(
                visible = transactionDetails.type == "Transfer",
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                val toAccount = accounts.find { it.id == transactionDetails.toAccountId }
                SelectionPopupCard(
                    label = stringResource(R.string.to_account),
                    selectedValue = toAccount?.name ?: stringResource(R.string.select_destination_account),
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    items = accounts.filter { it.id != transactionDetails.accountId }.map { it.name },
                    onItemSelected = { selectedName ->
                        val acc = accounts.find { it.name == selectedName }
                        if (acc != null) {
                            onValueChange(transactionDetails.copy(toAccountId = acc.id))
                        }
                    }
                )
            }

            // Animated Category Selector
            AnimatedVisibility(
                visible = transactionDetails.type != "Transfer",
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                SelectionPopupCard(
                    label = stringResource(R.string.category),
                    selectedValue = transactionDetails.category.ifBlank { stringResource(R.string.select_category) },
                    icon = Icons.Default.Category,
                    items = categoryNames.ifEmpty { listOf("No categories") }, // Fallback string since resolving inside composable non-composable is tricky
                    onItemSelected = { selectedCategory ->
                        if (selectedCategory != "No categories") {
                            onValueChange(transactionDetails.copy(category = selectedCategory))
                        }
                    }
                )
            }

            // Date Selection
            var showDatePicker by remember { mutableStateOf(false) }
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = transactionDetails.date)

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

            SelectionCard(
                label = stringResource(R.string.date),
                value = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(transactionDetails.date)),
                icon = Icons.Default.CalendarToday,
                onClick = { showDatePicker = true }
            )

            // Notes Input Field
            NotesInputField(
                notes = transactionDetails.notes,
                onNotesChange = { onValueChange(transactionDetails.copy(notes = it)) }
            )
            
            // Padding for FAB at the bottom
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun TypeSegmentedControl(
    selectedType: String,
    activeColor: Color,
    onTypeSelected: (String) -> Unit
) {
    val types = listOf("Expense", "Income", "Transfer")
    val stringTypes = listOf(stringResource(R.string.expense), stringResource(R.string.income), stringResource(R.string.transfer))

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth().height(48.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            types.forEachIndexed { index, type ->
                val isSelected = selectedType == type
                val backgroundColor by animateColorAsState(
                    targetValue = if (isSelected) activeColor else Color.Transparent,
                    animationSpec = tween(300), label = "SegmentColor"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(300), label = "SegmentTextColor"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(backgroundColor)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onTypeSelected(type) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringTypes[index],
                        color = textColor,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
fun SelectionPopupCard(
    label: String,
    selectedValue: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    items: List<String>,
    onItemSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        SelectionCard(
            label = label,
            value = selectedValue,
            icon = icon,
            onClick = { expanded = true }
        )
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f).background(MaterialTheme.colorScheme.surface)
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item, style = MaterialTheme.typography.bodyLarge) },
                    onClick = {
                        onItemSelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun SelectionCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon container
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun NotesInputField(
    notes: String,
    onNotesChange: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Notes,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.notes_optional),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                BasicTextField(
                    value = notes,
                    onValueChange = onNotesChange,
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Normal
                    ),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    decorationBox = { innerTextField ->
                        if (notes.isEmpty()) {
                            Text(
                                text = "Add a quick note...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                        innerTextField()
                    }
                )
            }
        }
    }
}
