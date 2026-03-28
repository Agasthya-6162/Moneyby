package com.example.moneyby.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.res.stringResource
import com.example.moneyby.R
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.moneyby.data.Transaction
import com.example.moneyby.ui.AppViewModelProvider
import com.example.moneyby.ui.theme.ExpenseRed
import com.example.moneyby.ui.theme.IncomeGreen
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import com.example.moneyby.ui.util.shimmerEffect
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onAddTransactionClick: () -> Unit,
    onViewAllClick: () -> Unit,
    onBudgetClick: () -> Unit,
    onGoalsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onStatisticsClick: () -> Unit,
    onRemindersClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val event by viewModel.event.collectAsStateWithLifecycle()
    val dashboardState by viewModel.dashboardUiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(event) {
        when (val e = event) {
            is DashboardEvent.Error -> {
                snackbarHostState.showSnackbar(e.message)
                viewModel.consumeEvent()
            }
            null -> Unit
        }
    }

    Scaffold(
        modifier = modifier.safeDrawingPadding(),
        topBar = {
            DashboardTopBar(
                onStatisticsClick = onStatisticsClick,
                onRemindersClick = onRemindersClick,
                onSettingsClick = onSettingsClick
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddTransactionClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp)) },
                text = { Text(stringResource(R.string.add_transaction), style = MaterialTheme.typography.labelLarge) },
                expanded = true
            )
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = dashboardState.isLoading,
            transitionSpec = {
                fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400))
            },
            label = "DashboardStateTransition"
        ) { isScreenLoading ->
            if (isScreenLoading) {
                DashboardShimmer(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                )
            } else {
                DashboardContent(
                    uiState = dashboardState,
                    onAddTransactionClick = onAddTransactionClick,
                    onViewAllClick = onViewAllClick,
                    onGoalsClick = onGoalsClick,
                    onBudgetClick = onBudgetClick,
                    onStatisticsClick = onStatisticsClick,
                    onSettingsClick = onSettingsClick,
                    onConfirmTransaction = { viewModel.confirmTransaction(it) },
                    onDiscardTransaction = { viewModel.discardTransaction(it) },
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun DashboardShimmer(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .shimmerEffect()
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                repeat(4) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .shimmerEffect()
                        )
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(10.dp)
                                .clip(MaterialTheme.shapes.small)
                                .shimmerEffect()
                        )
                    }
                }
            }
        }

        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(16.dp)
                        .clip(MaterialTheme.shapes.small)
                        .shimmerEffect()
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    repeat(2) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(120.dp)
                                .clip(MaterialTheme.shapes.extraLarge)
                                .shimmerEffect()
                        )
                    }
                }
            }
        }

        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(16.dp)
                        .clip(MaterialTheme.shapes.small)
                        .shimmerEffect()
                )
                Spacer(Modifier.height(16.dp))
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .padding(vertical = 6.dp)
                            .clip(MaterialTheme.shapes.large)
                            .shimmerEffect()
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardTopBar(
    onStatisticsClick: () -> Unit,
    onRemindersClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier.size(54.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        actions = {
            IconButton(onClick = onRemindersClick) {
                Icon(Icons.Default.NotificationsNone, contentDescription = stringResource(R.string.reminders))
            }
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
        )
    )
}

@Composable
fun DashboardContent(
    uiState: DashboardUiState,
    onAddTransactionClick: () -> Unit,
    onViewAllClick: () -> Unit,
    onGoalsClick: () -> Unit,
    onBudgetClick: () -> Unit,
    onStatisticsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onConfirmTransaction: (com.example.moneyby.data.PendingTransaction) -> Unit,
    onDiscardTransaction: (com.example.moneyby.data.PendingTransaction) -> Unit,
    modifier: Modifier = Modifier
) {
    val currencyFormatter = remember {
        java.text.NumberFormat.getCurrencyInstance(java.util.Locale.forLanguageTag("en-IN"))
    }
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("MMM dd").withZone(ZoneId.systemDefault())
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            NetWorthCard(
                netWorth = uiState.netWorth,
                monthlyExpense = uiState.monthlyExpense,
                monthlyIncome = uiState.monthlyIncome,
                currencyFormatter = currencyFormatter,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        item {
            QuickActionsRow(
                onAddClick = onAddTransactionClick,
                onBudgetClick = onBudgetClick,
                onGoalsClick = onGoalsClick,
                onStatsClick = onStatisticsClick
            )
        }


        if (uiState.pendingTransactions.isNotEmpty()) {
            item {
                PendingApprovalsSection(
                    pendingTransactions = uiState.pendingTransactions,
                    onConfirm = onConfirmTransaction,
                    onDiscard = onDiscardTransaction,
                    currencyFormatter = currencyFormatter
                )
            }
        }


        item {
            AccountsSection(
                accountBalances = uiState.accountBalances,
                onManageClick = onSettingsClick,
                currencyFormatter = currencyFormatter
            )
        }

        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    stringResource(R.string.overview),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1.1f)) {
                        MonthlyInsightCard(
                            topCategory = uiState.topSpendingCategory,
                            categoryAmount = uiState.categoryWiseSpending[uiState.topSpendingCategory]
                                ?: 0.0,
                            budgetsAtRiskCount = uiState.budgetsAtRiskCount,
                            currencyFormatter = currencyFormatter
                        )
                    }
                    if (uiState.categoryWiseSpending.isNotEmpty()) {
                        Box(modifier = Modifier.weight(0.9f)) {
                            SpendingPieChartCard(categorySpending = uiState.categoryWiseSpending)
                        }
                    }
                }
            }
        }

        if (uiState.savingGoals.isNotEmpty()) {
            item {
                GoalsSection(
                    goals = uiState.savingGoals,
                    onSeeAllClick = onGoalsClick,
                    currencyFormatter = currencyFormatter
                )
            }
        }

        item {
            RecentTransactionsSection(
                transactions = uiState.recentTransactions,
                onViewAllClick = onViewAllClick,
                currencyFormatter = currencyFormatter,
                dateFormatter = dateFormatter
            )
        }
    }
}

@Composable
fun AccountsSection(
    accountBalances: List<AccountBalance>,
    onManageClick: () -> Unit,
    currencyFormatter: NumberFormat
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isWideScreen = configuration.screenWidthDp >= 600
    val horizontalPadding = 16.dp
    val spacing = 12.dp
    
    // Adaptive grid width: 2 columns for phones, 3 or more for tablets
    val cardWidth = if (isWideScreen) 200.dp else (screenWidth - (horizontalPadding * 2) - spacing) / 2f

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.accounts),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            TextButton(onClick = onManageClick) {
                Text(stringResource(R.string.manage), style = MaterialTheme.typography.labelLarge)
            }
        }

        if (accountBalances.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.no_accounts_found),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        } else {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing),
                contentPadding = PaddingValues(horizontal = horizontalPadding)
            ) {
                items(accountBalances, key = { it.account.id }) { balance ->
                    AccountCard(
                        balance = balance,
                        currencyFormatter = currencyFormatter,
                        width = cardWidth
                    )
                }
            }
        }
    }
}

@Composable
fun GoalsSection(
    goals: List<com.example.moneyby.data.SavingGoal>,
    onSeeAllClick: () -> Unit,
    currencyFormatter: NumberFormat
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.saving_goals),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            TextButton(onClick = onSeeAllClick) {
                Text(stringResource(R.string.see_all), style = MaterialTheme.typography.labelLarge)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            goals.take(2).forEach { goal ->
                SavingGoalSummary(goal = goal, currencyFormatter = currencyFormatter)
            }
        }
    }
}

@Composable
fun RecentTransactionsSection(
    transactions: List<Transaction>,
    onViewAllClick: () -> Unit,
    currencyFormatter: NumberFormat,
    dateFormatter: DateTimeFormatter
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.recent_transactions),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            TextButton(onClick = onViewAllClick) {
                Text(stringResource(R.string.history), style = MaterialTheme.typography.labelLarge)
            }
        }

        if (transactions.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.no_transactions_yet),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.add_first_transaction),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.extraLarge,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            ) {
                Column {
                    transactions.take(5).forEachIndexed { index, transaction ->
                        TransactionItem(
                            transaction = transaction,
                            currencyFormatter = currencyFormatter,
                            dateFormatter = dateFormatter
                        )
                        if (index < transactions.size - 1 && index < 4) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionsRow(
    onAddClick: () -> Unit,
    onBudgetClick: () -> Unit,
    onGoalsClick: () -> Unit,
    onStatsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        QuickActionButton(
            icon = Icons.Default.AddCircleOutline,
            label = stringResource(R.string.expense),
            onClick = onAddClick,
            color = ExpenseRed
        )
        QuickActionButton(
            icon = Icons.Default.Timer,
            label = stringResource(R.string.budgets),
            onClick = onBudgetClick,
            color = MaterialTheme.colorScheme.secondary
        )
        QuickActionButton(
            icon = Icons.Default.Savings,
            label = stringResource(R.string.goals),
            onClick = onGoalsClick,
            color = IncomeGreen
        )
        QuickActionButton(
            icon = Icons.Default.BarChart,
            label = stringResource(R.string.stats),
            onClick = onStatsClick,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    color: Color
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.9f else 1f, label = "ButtonScale")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.1f),
            modifier = Modifier.size(52.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun NetWorthCard(
    netWorth: Double,
    monthlyExpense: Double,
    monthlyIncome: Double,
    currencyFormatter: NumberFormat,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    )
                )
                .fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    stringResource(R.string.total_net_worth),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
                Text(
                    currencyFormatter.format(netWorth),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimary
                )

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MiniSummary(
                        icon = Icons.Default.ArrowUpward,
                        label = stringResource(R.string.income),
                        amount = monthlyIncome,
                        formatter = currencyFormatter,
                        modifier = Modifier.weight(1f)
                    )
                    MiniSummary(
                        icon = Icons.Default.ArrowDownward,
                        label = stringResource(R.string.expense),
                        amount = monthlyExpense,
                        formatter = currencyFormatter,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun MiniSummary(
    icon: ImageVector,
    label: String,
    amount: Double,
    formatter: NumberFormat,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon, 
                contentDescription = null, 
                tint = MaterialTheme.colorScheme.onPrimary, 
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                )
                Text(
                    formatter.format(amount),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun MonthlyInsightCard(
    topCategory: String?,
    categoryAmount: Double,
    budgetsAtRiskCount: Int,
    currencyFormatter: NumberFormat,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                stringResource(R.string.insight),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))

            val insightText = if (topCategory != null) {
                buildAnnotatedString {
                    append(stringResource(R.string.highest_spending_in))
                    append(" ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(topCategory)
                    }
                    append(" (${currencyFormatter.format(categoryAmount)}).")
                }
            } else {
                buildAnnotatedString {
                    append(stringResource(R.string.no_spending_data))
                }
            }

            Text(
                text = insightText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = androidx.compose.ui.unit.TextUnit.Unspecified
            )

            if (budgetsAtRiskCount > 0) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        stringResource(R.string.budgets_near_limit, budgetsAtRiskCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun SpendingPieChartCard(categorySpending: Map<String, Double>) {
    val colors = com.example.moneyby.ui.theme.ChartColors

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .height(70.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                SpendingPieChart(spending = categorySpending)
            }

            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                categorySpending.entries
                    .sortedByDescending { it.value }
                    .take(3)
                    .forEachIndexed { index, entry ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(
                                        colors[index % colors.size],
                                        CircleShape
                                    )
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                entry.key,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
            }
        }
    }
}

@Composable
fun SpendingPieChart(spending: Map<String, Double>) {
    val total = spending.values.sum()
    val colors = com.example.moneyby.ui.theme.ChartColors
    
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(spending) {
        animatedProgress.animateTo(1f, animationSpec = tween(1000, easing = FastOutSlowInEasing))
    }

    androidx.compose.foundation.Canvas(modifier = Modifier.size(60.dp)) {
        var startAngle = -90f
        spending.entries.sortedByDescending { it.value }.take(5).forEachIndexed { index, entry ->
            val sweepAngle = ((entry.value / total).toFloat() * 360f) * animatedProgress.value
            drawArc(
                color = colors[index % colors.size],
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = 12f, cap = StrokeCap.Round)
            )
            startAngle += (entry.value / total).toFloat() * 360f
        }
    }
}

@Composable
fun AccountCard(
    balance: AccountBalance,
    currencyFormatter: NumberFormat,
    width: Dp = 125.dp
) {
    Card(
        modifier = Modifier
            .width(width)
            .height(130.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            val icon = when (balance.account.type) {
                "Bank" -> Icons.Default.AccountBalance
                "Cash" -> Icons.Default.Payments
                "Credit Card" -> Icons.Default.CreditCard
                else -> Icons.Default.Wallet
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            icon,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    balance.account.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    currencyFormatter.format(balance.balance),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    stringResource(R.string.balance),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun TransactionItem(
    transaction: Transaction,
    currencyFormatter: NumberFormat,
    dateFormatter: DateTimeFormatter
) {
    val categoryIcon = when (transaction.category) {
        "Food" -> Icons.Default.Restaurant
        "Transport" -> Icons.Default.DirectionsCar
        "Shopping" -> Icons.Default.ShoppingBag
        "Health" -> Icons.Default.MedicalServices
        "Entertainment" -> Icons.Default.Movie
        "Utilities" -> Icons.Default.Bolt
        "Salary" -> Icons.Default.Payments
        else -> Icons.Default.Category
    }

    val color = if (transaction.type == "Expense") ExpenseRed else IncomeGreen

    ListItem(
        headlineContent = {
            Text(
                transaction.category,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        supportingContent = {
            val dateStr = dateFormatter.format(Instant.ofEpochMilli(transaction.date))
            Text(
                "$dateStr${if (!transaction.notes.isNullOrBlank()) " • ${transaction.notes}" else ""}",
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        trailingContent = {
            Text(
                (if (transaction.type == "Expense") "-" else "+") + currencyFormatter.format(transaction.amount),
                color = color,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    categoryIcon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
fun SavingGoalSummary(
    goal: com.example.moneyby.data.SavingGoal,
    currencyFormatter: NumberFormat
) {
    val progress = (goal.currentAmount / goal.targetAmount).coerceIn(0.0, 1.0).toFloat()
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "GoalProgress"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    goal.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${currencyFormatter.format(goal.currentAmount)} / ${
                        currencyFormatter.format(goal.targetAmount)
                    }",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                strokeCap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun PendingApprovalsSection(
    pendingTransactions: List<com.example.moneyby.data.PendingTransaction>,
    onConfirm: (com.example.moneyby.data.PendingTransaction) -> Unit,
    onDiscard: (com.example.moneyby.data.PendingTransaction) -> Unit,
    currencyFormatter: NumberFormat
) {
    if (pendingTransactions.isEmpty()) return

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.PendingActions,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.pending_approvals),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape,
                modifier = Modifier.size(24.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        pendingTransactions.size.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(pendingTransactions, key = { it.transactionHash }) { pending ->
                PendingTransactionItem(
                    pending = pending,
                    onConfirm = { onConfirm(pending) },
                    onDiscard = { onDiscard(pending) },
                    currencyFormatter = currencyFormatter
                )
            }
        }
    }
}

@Composable
fun PendingTransactionItem(
    pending: com.example.moneyby.data.PendingTransaction,
    onConfirm: () -> Unit,
    onDiscard: () -> Unit,
    currencyFormatter: NumberFormat
) {
    Card(
        modifier = Modifier
            .width(260.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        pending.merchant ?: stringResource(R.string.unknown_merchant),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        pending.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
                Text(
                    currencyFormatter.format(pending.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (pending.type == "Expense") ExpenseRed else IncomeGreen
                )
            }
            
            if (!pending.accountSuffix.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.05f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "A/c: ****${pending.accountSuffix}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDiscard,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.large,
                    contentPadding = PaddingValues(0.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Text(
                        stringResource(R.string.discard),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1.1f),
                    shape = MaterialTheme.shapes.large,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        stringResource(R.string.confirm),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
