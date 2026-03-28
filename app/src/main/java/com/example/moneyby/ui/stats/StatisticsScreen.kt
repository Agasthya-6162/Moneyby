@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.moneyby.ui.stats

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.moneyby.R
import com.example.moneyby.ui.AppViewModelProvider
import com.example.moneyby.ui.theme.ChartColors
import com.example.moneyby.ui.theme.IncomeGreen
import com.example.moneyby.ui.theme.ExpenseRed
import com.example.moneyby.util.formatCurrency
import java.util.*

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
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            stringResource(R.string.financial_insights),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$monthName $selectedYear",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.goToPreviousMonth() }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Prev")
                    }
                    IconButton(
                        onClick = { viewModel.goToNextMonth() },
                        enabled = !isCurrentMonth
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Premium Summary Header
            item {
                SummaryHeader(uiState.totalIncome, uiState.totalExpense)
            }

            // 2. Interactive Donut Chart for Expense Breakdown
            item {
                SectionTitle(stringResource(R.string.expense_breakdown))
                if (uiState.categoryBreakdown.isNotEmpty()) {
                    DonutChartCard(uiState.categoryBreakdown)
                } else {
                    EmptyStateCard(stringResource(R.string.no_expenses_tracked))
                }
            }

            // 3. Category Breakdown List with Percentage Bars
            if (uiState.categoryBreakdown.isNotEmpty()) {
                itemsIndexed(uiState.categoryBreakdown.toList().sortedByDescending { it.second }) { index, (category, amount) ->
                    CategoryBreakdownRow(
                        category = category,
                        amount = amount,
                        total = uiState.totalExpense,
                        color = ChartColors[index % ChartColors.size],
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }

            // 4. Trend Visualization
            item {
                SectionTitle(stringResource(R.string.trend_title))
                TrendChartCard(uiState.monthlyTrend)
            }
        }
    }
}

@Composable
fun SummaryHeader(income: Double, expense: Double) {
    val net = income - expense
    val netColor = if (net >= 0) IncomeGreen else ExpenseRed
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.net_balance),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                formatCurrency(net),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = netColor
            )
            
            Spacer(Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Income Mini-Card
                SummaryIndicator(
                    label = stringResource(R.string.income),
                    amount = income,
                    color = IncomeGreen,
                    icon = Icons.Default.TrendingUp,
                    modifier = Modifier.weight(1f)
                )
                
                // Expense Mini-Card
                SummaryIndicator(
                    label = stringResource(R.string.expense),
                    amount = expense,
                    color = ExpenseRed,
                    icon = Icons.Default.TrendingDown,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun SummaryIndicator(
    label: String,
    amount: Double,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = color)
                Text(
                    formatCurrency(amount),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 24.dp)
    )
}

@Composable
fun DonutChartCard(data: Map<String, Double>) {
    val total = data.values.sum()
    val animatedProgress = remember { Animatable(0f) }
    
    LaunchedEffect(data) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(1f, animationSpec = tween(1000, easing = FastOutSlowInEasing))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
                Canvas(modifier = Modifier.size(140.dp)) {
                    var startAngle = -90f
                    data.values.forEachIndexed { index, value ->
                        val sweepAngle = (value / total * 360f).toFloat() * animatedProgress.value
                        drawArc(
                            color = ChartColors[index % ChartColors.size],
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round)
                        )
                        startAngle += sweepAngle
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.total), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        formatCompactNumber(total),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(Modifier.width(24.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                data.toList().sortedByDescending { it.second }.take(4).forEachIndexed { index, (category, amount) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(ChartColors[index % ChartColors.size]))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            category,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.weight(1f),
                            maxLines = 1
                        )
                        Text(
                            "${(amount / total * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryBreakdownRow(
    category: String,
    amount: Double,
    total: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    val percentage = (amount / total).toFloat()
    
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(category, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(formatCurrency(amount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { percentage },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
            color = color,
            trackColor = color.copy(alpha = 0.1f),
            strokeCap = StrokeCap.Round
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "${(percentage * 100).toInt()}% of total expenses",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun TrendChartCard(trend: List<MonthlyTrend>) {
    val maxVal = (trend.flatMap { listOf(it.income, it.expense) }.maxOrNull() ?: 1.0).toFloat()
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(24.dp)) {
            Box(Modifier.height(200.dp).fillMaxWidth()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val spacing = width / (trend.size + 1)
                    
                    trend.forEachIndexed { index, data ->
                        val x = spacing * (index + 1)
                        
                        // Expense Bar (Primary container style)
                        val expHeight = (data.expense / maxVal * height * 0.8).toFloat()
                        drawRoundRect(
                            color = ExpenseRed.copy(alpha = 0.8f),
                            topLeft = Offset(x - 12.dp.toPx(), height - expHeight),
                            size = Size(10.dp.toPx(), expHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                        )

                        // Income Bar (Primary style)
                        val incHeight = (data.income / maxVal * height * 0.8).toFloat()
                        drawRoundRect(
                            color = IncomeGreen.copy(alpha = 0.8f),
                            topLeft = Offset(x + 2.dp.toPx(), height - incHeight),
                            size = Size(10.dp.toPx(), incHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
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
                LegendItem(stringResource(R.string.income), IncomeGreen)
                Spacer(Modifier.width(24.dp))
                LegendItem(stringResource(R.string.expense), ExpenseRed)
            }
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun EmptyStateCard(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(
            Modifier
                .padding(48.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

fun formatCompactNumber(number: Double): String {
    return when {
        number >= 1_000_000 -> String.format(Locale.getDefault(), "₹ %.1fM", number / 1_000_000)
        number >= 1_000 -> String.format(Locale.getDefault(), "₹ %.1fK", number / 1_000)
        else -> String.format(Locale.getDefault(), "₹ %.0f", number)
    }
}
