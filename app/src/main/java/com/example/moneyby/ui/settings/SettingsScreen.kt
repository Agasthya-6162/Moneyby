@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.moneyby.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedCard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.moneyby.ui.AppViewModelProvider
import androidx.compose.ui.res.stringResource
import com.example.moneyby.R

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToRecurring: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var importUri by remember { mutableStateOf<Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri -> uri?.let { viewModel.exportBackup(it) } }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            importUri = it
            showPasswordDialog = true
        }
    }

    val context = LocalContext.current
    val event by viewModel.event.collectAsState()

    LaunchedEffect(event) {
        event?.let { e ->
            when (e) {
                is SettingsEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(e.message)
                }
            }
            viewModel.consumeEvent()
        }
    }

    Scaffold(
        modifier = modifier.safeDrawingPadding(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold) },
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
                contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {

            // ── App Customisation ─────────────────────────────────────────────
            item {
                SettingsSection(title = stringResource(R.string.app_customization)) {
                    SettingsItem(
                        title    = stringResource(R.string.manage_categories),
                        subtitle = stringResource(R.string.manage_categories_subtitle),
                        icon     = Icons.Default.Category,
                        onClick  = onNavigateToCategories
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsItem(
                        title    = stringResource(R.string.recurring_transactions),
                        subtitle = stringResource(R.string.recurring_transactions_subtitle),
                        icon     = Icons.Default.Repeat,
                        onClick  = onNavigateToRecurring
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsItem(
                        title    = stringResource(R.string.manage_accounts),
                        subtitle = stringResource(R.string.manage_accounts_subtitle),
                        icon     = Icons.Default.AccountBalanceWallet,
                        onClick  = onNavigateToAccounts
                    )
                }
            }

            // ── Financial Rules ───────────────────────────────────────────────
            item {
                SettingsSection(title = stringResource(R.string.financial_rules)) {
                    var showResetDayDialog by remember { mutableStateOf(false) }
                    SettingsItem(
                        title    = stringResource(R.string.monthly_reset_day),
                        subtitle = stringResource(
                            R.string.current_reset_day,
                            uiState.resetDay,
                            getOrdinalIndicator(uiState.resetDay)
                        ),
                        icon    = Icons.Default.EventRepeat,
                        onClick = { showResetDayDialog = true }
                    )
                    if (showResetDayDialog) {
                        ResetDayDialog(
                            currentDay = uiState.resetDay,
                            onDismiss  = { showResetDayDialog = false },
                            onSelect   = { day ->
                                viewModel.setResetDay(day)
                                showResetDayDialog = false
                            }
                        )
                    }
                }
            }

            // ── Automation ────────────────────────────────────────────────────
            item {
                SettingsSection(title = stringResource(R.string.automation)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    stringResource(R.string.auto_detection_title),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    stringResource(R.string.auto_detection_subtitle),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = uiState.isAutoDetectionEnabled,
                                onCheckedChange = { viewModel.setAutoDetectionEnabled(it) }
                            )
                        }

                        AnimatedVisibility(
                            visible = uiState.isAutoDetectionEnabled,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column {
                                Spacer(Modifier.height(16.dp))
                                val ctx = LocalContext.current
                                OutlinedButton(
                                    onClick = {
                                        val intent = android.content.Intent(
                                            "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"
                                        )
                                        ctx.startActivity(intent)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Icon(
                                        Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.enable_notification_access))
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    stringResource(R.string.notification_access_info),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // ── Data Management ───────────────────────────────────────────────
            item {
                SettingsSection(title = stringResource(R.string.data_management)) {

                    Text(
                        stringResource(R.string.backup_info),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    Spacer(Modifier.height(4.dp))

                    // Auto-backup toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.auto_backup), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            Text(
                                stringResource(R.string.auto_backup_subtitle),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.isAutoBackupEnabled,
                            onCheckedChange = { viewModel.setAutoBackupEnabled(it) }
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // Export / Import buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                exportLauncher.launch("moneyby_backup_${System.currentTimeMillis()}.zip")
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !uiState.isLoading,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.backup))
                        }
                        OutlinedButton(
                            onClick = { importLauncher.launch("application/zip") },
                            modifier = Modifier.weight(1f),
                            enabled = !uiState.isLoading,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.restore))
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.exportToPublicFolder() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        enabled = !uiState.isLoading,
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.export_to_moneyby_folder))
                    }

                    Text(
                        stringResource(R.string.backup_to_folder_subtitle),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp)
                    )
                }
            }

            // ── Support ───────────────────────────────────────────────────────
            item {
                var showBugReportDialog by remember { mutableStateOf(false) }
                SettingsSection(title = stringResource(R.string.support)) {
                    SettingsItem(
                        title    = stringResource(R.string.report_bug),
                        subtitle = stringResource(R.string.bug_report_subtitle),
                        icon     = Icons.Default.BugReport,
                        onClick  = { showBugReportDialog = true }
                    )
                }
                if (showBugReportDialog) {
                    BugReportDialog(
                        appVersion = uiState.appVersion,
                        onDismiss  = { showBugReportDialog = false }
                    )
                }
            }

            // Loading indicator
            item {
                AnimatedVisibility(
                    visible = uiState.isLoading,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }

            // App version footer
            item {
                Spacer(Modifier.height(12.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        stringResource(R.string.app_version, uiState.appVersion),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.developed_by),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }

    // Restore password dialog
    if (showPasswordDialog) {
            var password by remember { mutableStateOf("") }
            var passVisible by remember { mutableStateOf(false) }
            AlertDialog(
                onDismissRequest = {
                    showPasswordDialog = false
                    importUri = null
                },
                title = { Text(stringResource(R.string.restore_protected_backup)) },
                text = {
                    Column {
                        Text(stringResource(R.string.restore_password_info))
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text(stringResource(R.string.password)) },
                            singleLine = true,
                            visualTransformation = if (passVisible) VisualTransformation.None
                                                   else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { passVisible = !passVisible }) {
                                    Icon(
                                        if (passVisible) Icons.Default.VisibilityOff
                                        else Icons.Default.Visibility,
                                        contentDescription = null
                                    )
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
                            importUri?.let { viewModel.importBackup(it, password) }
                            showPasswordDialog = false
                            importUri = null
                        },
                        shape = MaterialTheme.shapes.medium
                    ) { Text(stringResource(R.string.restore_now)) }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showPasswordDialog = false
                        importUri = null
                    }) { Text(stringResource(R.string.cancel)) }
                }
            )
        }
    }
}

// ── Reusable components ────────────────────────────────────────────────────────

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text  = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
            fontWeight = FontWeight.Bold
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape    = MaterialTheme.shapes.extraLarge,
            border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color   = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun ResetDayDialog(
    currentDay: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.monthly_reset_day)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.select_reset_day_info),
                    style    = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier.height(200.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement   = Arrangement.spacedBy(8.dp)
                ) {
                    items((1..28).toList()) { day ->
                        Surface(
                            onClick = { onSelect(day) },
                            shape  = CircleShape,
                            color  = if (day == currentDay) MaterialTheme.colorScheme.primary else Color.Transparent,
                            border = if (day == currentDay) null
                                     else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    day.toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (day == currentDay) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (day == currentDay) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
fun BugReportDialog(
    appVersion: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var reportType  by remember { mutableStateOf("Bug") }
    var description by remember { mutableStateOf("") }
    var expanded    by remember { mutableStateOf(false) }

    val types = listOf(
        stringResource(R.string.bug),
        stringResource(R.string.error),
        stringResource(R.string.enhancement)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.bug_report_details)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column {
                    Text(stringResource(R.string.report_type), style = MaterialTheme.typography.labelMedium)
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        OutlinedCard(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(reportType)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            types.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type) },
                                    onClick = { reportType = type; expanded = false }
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.describe_issue)) },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    placeholder = { Text(stringResource(R.string.describe_issue_placeholder)) },
                    shape = MaterialTheme.shapes.medium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                        data = android.net.Uri.parse("mailto:agasthya.me@gmail.com")
                        putExtra(android.content.Intent.EXTRA_SUBJECT, "Moneyby Support Request: $reportType")
                        putExtra(
                            android.content.Intent.EXTRA_TEXT,
                            "Type: $reportType\n\nDetails:\n$description\n\nApp Version: $appVersion"
                        )
                    }
                    try { context.startActivity(intent) } catch (_: Exception) {}
                    onDismiss()
                },
                enabled = description.isNotBlank(),
                shape = MaterialTheme.shapes.medium
            ) { Text(stringResource(R.string.submit)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

fun getOrdinalIndicator(day: Int): String {
    if (day in 11..13) return "th"
    return when (day % 10) {
        1 -> "st"; 2 -> "nd"; 3 -> "rd"; else -> "th"
    }
}
