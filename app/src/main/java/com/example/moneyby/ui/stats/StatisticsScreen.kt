@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.moneyby.ui.stats

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import java.util.Calendar
import java.util.Locale
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.moneyby.ui.AppViewModelProvider
import androidx.compose.ui.res.stringResource
import com.example.moneyby.R
import com.example.moneyby.ui.theme.ChartColors
import java.text.NumberFormat
import com.example.moneyby.util.formatCurrency

@Composable
fun StatisticsScreen(
    onBackClick: () -> Unit,
    viewModel: StatisticsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.statsUiState.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()

    val now = Calendar.getInstance()
    val isCurrentMonth = selectedMonth == now.get(Calendar.MONTH) &&
            selectedYear == now.get(Calendar.YEAR)

    val monthName = Calendar.getInstance().apply {
        set(Calendar.MONTH, selectedMonth)
        set(Calendar.YEAR, selectedYear)
    }.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) ?: ""

    val snackbarHostState = remember { SnackbarHostState() }
    val event by viewModel.event.collectAsState()

    LaunchedEffect(event) {
        event?.let { e ->
            when (e) {
                is StatisticsEvent.ShowError -> {
                    snackbarHostState.showSnackbar(e.message)
                }
            }
            viewModel.consumeEvent()
        }
    }

    Scaffold(
        modifier = Modifier.safeDrawingPadding(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.financial_insights)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
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
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.goToPreviousMonth() }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                        }
                        Text(
                            text = "$monthName $selectedYear",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { viewModel.goToNextMonth() },
                            enabled = !isCurrentMonth
                        ) {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = "Next Month",
                                tint = if (isCurrentMonth)
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                item { SummaryCard(uiState.totalIncome, uiState.totalExpense) }

                item {
                    Text(
                        stringResource(R.string.expense_breakdown),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.height(12.dp))
                    if (uiState.categoryBreakdown.isNotEmpty()) {
                        PieChart(uiState.categoryBreakdown)
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Box(
                                Modifier.padding(48.dp).fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(stringResource(R.string.no_expenses_tracked),
                                    style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                item {
                    Text(
                        stringResource(R.string.trend_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.height(12.dp))
                    TrendChart(uiState.monthlyTrend)
                }
            }
        }
    }
}

@Composable
fun SummaryCard(income: Double, expense: Double) {
    val net = income - expense
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(stringResource(R.string.monthly_income), style = MaterialTheme.typography.labelMedium)
                    Text(
                        formatCurrency(income),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.monthly_expense), style = MaterialTheme.typography.labelMedium)
                    Text(
                        formatCurrency(expense),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f))
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.net_balance), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    formatCurrency(net),
                    style = MaterialTheme.typography.titleLarge,
                    color = if (net >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun PieChart(data: Map<String, Double>) {
    val total = data.values.sum()

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
                    Canvas(modifier = Modifier.size(140.dp)) {
                        var startAngle = -90f
                        data.values.forEachIndexed { index, value ->
                            val sweepAngle = (value / total * 360f).toFloat()
                            drawArc(
                                color = ChartColors[index % ChartColors.size],
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(width = 30.dp.toPx(), cap = StrokeCap.Round)
                            )
                            startAngle += sweepAngle
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.total), style = MaterialTheme.typography.labelSmall)
                        Text(
                            formatCompactNumber(total),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(Modifier.width(28.dp))
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    data.keys.take(6).forEachIndexed { index, category ->
                        val amount = data[category] ?: 0.0
                        val percentage = (amount / total * 100).toInt()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                Modifier.size(12.dp), 
                                color = ChartColors[index % ChartColors.size], 
                                shape = MaterialTheme.shapes.extraSmall
                            ) {}
                            Spacer(Modifier.width(8.dp))
                            Text(category, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            Text("$percentage%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrendChart(trend: List<MonthlyTrend>) {
    val maxVal = (trend.flatMap { listOf(it.income, it.expense) }.maxOrNull() ?: 1.0).toFloat()
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Box(Modifier.height(200.dp).fillMaxWidth()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val spacing = width / (trend.size + 1)
                    
                    trend.forEachIndexed { index, data ->
                        val x = spacing * (index + 1)
                        
                        // Expense Bar (Primary container style)
                        val expHeight = (data.expense / maxVal * height * 0.7).toFloat()
                        drawRect(
                            color = com.example.moneyby.ui.theme.ExpenseRed.copy(alpha = 0.6f),
                            topLeft = Offset(x - 12.dp.toPx(), height - expHeight),
                            size = Size(10.dp.toPx(), expHeight)
                        )

                        // Income Bar (Primary style)
                        val incHeight = (data.income / maxVal * height * 0.7).toFloat()
                        drawRect(
                            color = com.example.moneyby.ui.theme.IncomeGreen.copy(alpha = 0.6f),
                            topLeft = Offset(x + 2.dp.toPx(), height - incHeight),
                            size = Size(10.dp.toPx(), incHeight)
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                trend.forEach { data ->
                    Text(
                        data.monthName, 
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(Modifier.size(8.dp), color = com.example.moneyby.ui.theme.IncomeGreen, shape = MaterialTheme.shapes.extraSmall) {}
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.income), style = MaterialTheme.typography.labelSmall)
                }
                Spacer(Modifier.width(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(Modifier.size(8.dp), color = com.example.moneyby.ui.theme.ExpenseRed, shape = MaterialTheme.shapes.extraSmall) {}
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.expense), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

fun formatCompactNumber(number: Double): String {
    return when {
        number >= 1_000_000 -> String.format(Locale.getDefault(), "%.1fM", number / 1_000_000)
        number >= 1_000 -> String.format(Locale.getDefault(), "%.1fK", number / 1_000)
        else -> String.format(Locale.getDefault(), "%.0f", number)
    }
}
