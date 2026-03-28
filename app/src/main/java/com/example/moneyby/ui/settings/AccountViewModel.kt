package com.example.moneyby.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneyby.data.Account
import com.example.moneyby.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AccountViewModel(private val transactionRepository: TransactionRepository) : ViewModel() {

    private val _event = MutableStateFlow<AccountEvent?>(null)
    val event: StateFlow<AccountEvent?> = _event.asStateFlow()

    fun consumeEvent() { _event.value = null }

    val accountsState: StateFlow<AccountUiState> = transactionRepository.getAllAccountsStream()
        .map { AccountUiState(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AccountUiState()
        )

    fun saveAccount(name: String, type: String, initialBalance: Double, suffix: String? = null, id: Int = 0) {
        viewModelScope.launch {
            val account = Account(
                id = id, 
                name = name, 
                type = type, 
                initialBalance = initialBalance,
                accountNumberSuffix = suffix?.takeIf { it.isNotBlank() }
            )
            val result = if (id == 0) {
                transactionRepository.insertAccount(account)
            } else {
                transactionRepository.updateAccount(account)
            }
            
            if (result is com.example.moneyby.util.Result.Success) {
                _event.value = AccountEvent.ShowMessage("Account saved successfully!")
            } else if (result is com.example.moneyby.util.Result.Error) {
                _event.value = AccountEvent.ShowError(result.message)
            }
        }
    }

    fun deleteAccount(account: Account) {
        viewModelScope.launch {
            val result = transactionRepository.deleteAccount(account)
            if (result is com.example.moneyby.util.Result.Success) {
                _event.value = AccountEvent.ShowMessage("Account deleted successfully!")
            } else if (result is com.example.moneyby.util.Result.Error) {
                _event.value = AccountEvent.ShowError(result.message)
            }
        }
    }
}

sealed class AccountEvent {
    data class ShowMessage(val message: String) : AccountEvent()
    data class ShowError(val message: String) : AccountEvent()
}

data class AccountUiState(
    val accounts: List<Account> = emptyList()
)
