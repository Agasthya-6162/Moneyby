package com.example.moneyby

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.moneyby.ui.auth.AuthScreen
import com.example.moneyby.ui.budget.BudgetScreen
import com.example.moneyby.ui.dashboard.DashboardScreen
import com.example.moneyby.ui.history.TransactionHistoryScreen
import com.example.moneyby.ui.goal.SavingGoalScreen
import com.example.moneyby.ui.settings.CategoryManagerScreen
import com.example.moneyby.ui.settings.SettingsScreen
import com.example.moneyby.ui.theme.MoneybyTheme
import com.example.moneyby.ui.transaction.TransactionEntryScreen

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Prevent screenshots and screen recording for security
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        
        val app = (application as? MoneybyApplication)
        val securityManager = app?.securityManager ?: return // Fail gracefully if app is not correct
        
        setContent {
            MoneybyTheme {
                // Request permissions
                val autoDetectionEnabled by securityManager.isAutoDetectionEnabled.collectAsState(initial = false)
                
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) {}

                LaunchedEffect(autoDetectionEnabled) {
                    val permissions = mutableListOf<String>()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    if (autoDetectionEnabled) {
                        permissions.add(Manifest.permission.RECEIVE_SMS)
                    }
                    if (permissions.isNotEmpty()) {
                        permissionLauncher.launch(permissions.toTypedArray())
                    }
                }


                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var isInitialized by remember { mutableStateOf(false) }
                    var isAuthenticated by remember { mutableStateOf(false) }

                    LaunchedEffect(Unit) {
                        val app = application as? MoneybyApplication
                        if (app != null) {
                            try {
                                val appContainer = app.container as? AppDataContainer
                                if (appContainer != null) {
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                        appContainer.ensureInitialized()
                                    }
                                }
                                isInitialized = true
                            } catch (e: Exception) {
                                e.printStackTrace()
                                isInitialized = true
                            }
                        } else {
                            isInitialized = true
                        }
                    }

                    if (!isInitialized) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                        return@Surface
                    }

                    AnimatedContent(
                        targetState = isAuthenticated,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
                        },
                        label = "AuthTransition"
                    ) { authenticated ->
                        if (!authenticated) {
                            AuthScreen(
                                securityManager = securityManager,
                                onAuthenticated = { isAuthenticated = true }
                            )
                        } else {
                            val navController = rememberNavController()
                            NavHost(
                                navController = navController,
                                startDestination = "dashboard",
                                enterTransition = {
                                    slideIntoContainer(
                                        AnimatedContentTransitionScope.SlideDirection.Left,
                                        animationSpec = tween(400)
                                    ) + fadeIn(animationSpec = tween(400))
                                },
                                exitTransition = {
                                    slideOutOfContainer(
                                        AnimatedContentTransitionScope.SlideDirection.Left,
                                        animationSpec = tween(400)
                                    ) + fadeOut(animationSpec = tween(400))
                                },
                                popEnterTransition = {
                                    slideIntoContainer(
                                        AnimatedContentTransitionScope.SlideDirection.Right,
                                        animationSpec = tween(400)
                                    ) + fadeIn(animationSpec = tween(400))
                                },
                                popExitTransition = {
                                    slideOutOfContainer(
                                        AnimatedContentTransitionScope.SlideDirection.Right,
                                        animationSpec = tween(400)
                                    ) + fadeOut(animationSpec = tween(400))
                                }
                            ) {
                                composable("dashboard") {
                                    DashboardScreen(
                                        onAddTransactionClick = {
                                            navController.navigate("transaction_entry")
                                        },
                                        onViewAllClick = {
                                            navController.navigate("history")
                                        },
                                        onBudgetClick = {
                                            navController.navigate("budget")
                                        },
                                        onGoalsClick = {
                                            navController.navigate("saving_goals")
                                        },
                                        onSettingsClick = {
                                            navController.navigate("settings")
                                        },
                                        onStatisticsClick = {
                                            navController.navigate("statistics")
                                        },
                                        onRemindersClick = {
                                            navController.navigate("bill_reminders")
                                        }
                                    )
                                }
                                composable("transaction_entry") {
                                    TransactionEntryScreen(
                                        navigateBack = { navController.popBackStack() }
                                    )
                                }
                                composable("transaction_edit/{transactionId}") { backStackEntry ->
                                    val id = backStackEntry.arguments?.getString("transactionId")?.toIntOrNull() ?: 0
                                    TransactionEntryScreen(
                                        navigateBack = { navController.popBackStack() },
                                        transactionId = id
                                    )
                                }
                                composable("history") {
                                    TransactionHistoryScreen(
                                        onBackClick = { navController.popBackStack() },
                                        onEditClick = { id ->
                                            navController.navigate("transaction_edit/$id")
                                        }
                                    )
                                }
                                composable("budget") {
                                    BudgetScreen(
                                        onBackClick = { navController.popBackStack() }
                                    )
                                }
                                composable("saving_goals") {
                                    SavingGoalScreen(
                                        onBackClick = { navController.popBackStack() }
                                    )
                                }
                                composable("settings") {
                                    SettingsScreen(
                                        onBackClick = { navController.popBackStack() },
                                        onNavigateToCategories = { navController.navigate("category_management") },
                                        onNavigateToRecurring = { navController.navigate("recurring_transactions") },
                                        onNavigateToAccounts = { navController.navigate("account_management") }
                                    )
                                }
                                composable("category_management") {
                                    CategoryManagerScreen(
                                        navigateBack = { navController.popBackStack() }
                                    )
                                }
                                composable("account_management") {
                                    com.example.moneyby.ui.settings.AccountManagementScreen(
                                        onBackClick = { navController.popBackStack() }
                                    )
                                }
                                composable("recurring_transactions") {
                                    com.example.moneyby.ui.recurring.RecurringTransactionScreen(
                                        onBackClick = { navController.popBackStack() }
                                    )
                                }
                                composable("statistics") {
                                    com.example.moneyby.ui.stats.StatisticsScreen(
                                        onBackClick = { navController.popBackStack() }
                                    )
                                }
                                composable("bill_reminders") {
                                    com.example.moneyby.ui.reminder.BillReminderScreen(
                                        onBackClick = { navController.popBackStack() }
                                    )
                                }


                                composable("change_pin") {
                                    AuthScreen(
                                        securityManager = securityManager,
                                        onAuthenticated = { navController.popBackStack() }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
