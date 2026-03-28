@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.moneyby.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.moneyby.data.SecurityManager
import kotlinx.coroutines.launch
import androidx.biometric.BiometricPrompt
import androidx.compose.ui.res.stringResource
import com.example.moneyby.R

@Composable
fun AuthScreen(
    securityManager: SecurityManager,
    onAuthenticated: () -> Unit,
    modifier: Modifier = Modifier
) {
    val savedPin by securityManager.appPin.collectAsStateWithLifecycle(initialValue = null)
    val isBiometricEnabled by securityManager.isBiometricEnabled.collectAsStateWithLifecycle(initialValue = false)
    var enteredPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    
    val context = LocalContext.current
    val executor = remember { ContextCompat.getMainExecutor(context) }
    val biometricPrompt = remember {
        BiometricPrompt(
            context as FragmentActivity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onAuthenticated()
                }
            }
        )
    }

    val promptInfo = remember {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(context.getString(R.string.biometric_login))
            .setSubtitle(context.getString(R.string.biometric_subtitle))
            .setNegativeButtonText(context.getString(R.string.use_pin))
            .build()
    }

    LaunchedEffect(savedPin, isBiometricEnabled) {
        if (savedPin != null && isBiometricEnabled && securityManager.isBiometricAvailable()) {
            biometricPrompt.authenticate(promptInfo)
        }
    }

    val isSetup = savedPin == null

    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (isSetup) stringResource(R.string.set_app_pin) else stringResource(R.string.enter_pin_to_unlock),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = enteredPin,
            onValueChange = { 
                if (it.length <= 4) {
                    enteredPin = it
                    error = null
                }
            },
            label = { Text(stringResource(R.string.pin_label)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.width(200.dp),
            isError = error != null,
            singleLine = true
        )
        
        error?.let { errorMessage ->
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        val coroutineScope = rememberCoroutineScope()
        Button(
            onClick = {
                if (enteredPin.length == 4) {
                    if (isSetup) {
                        coroutineScope.launch {
                            securityManager.setPin(enteredPin)
                            onAuthenticated()
                        }
                    } else {
                        coroutineScope.launch {
                            if (securityManager.verifyPin(enteredPin)) {
                                onAuthenticated()
                            } else {
                                error = context.getString(R.string.incorrect_pin)
                                enteredPin = ""
                            }
                        }
                    }
                }
            },
            enabled = enteredPin.length == 4,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isSetup) stringResource(R.string.set_pin) else stringResource(R.string.unlock))
        }

        if (!isSetup && securityManager.isBiometricAvailable()) {
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = { biometricPrompt.authenticate(promptInfo) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Fingerprint, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.login_with_fingerprint))
            }
        }
    }
}
