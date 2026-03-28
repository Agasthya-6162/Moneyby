package com.example.moneyby.ui.reminder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneyby.data.BillReminder
import com.example.moneyby.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BillReminderViewModel(private val transactionRepository: TransactionRepository) : ViewModel() {

    val uiState: StateFlow<List<BillReminder>> = transactionRepository.getAllBillRemindersStream()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // ── One-shot events ───────────────────────────────────────────────────────
    private val _event = MutableStateFlow<BillReminderEvent?>(null)
    val event: StateFlow<BillReminderEvent?> = _event.asStateFlow()
    fun consumeEvent() { _event.value = null }

    fun saveReminder(name: String, amount: Double, dueDate: Long, category: String, reminderDays: Int) {
        viewModelScope.launch {
            val reminder = BillReminder(
                name = name,
                amount = amount,
                dueDate = dueDate,
                category = category,
                reminderDaysBefore = reminderDays
            )
            val result = transactionRepository.insertBillReminder(reminder)
            if (result is com.example.moneyby.util.Result.Error) {
                _event.value = BillReminderEvent.Error(result.message)
            }
        }
    }

    fun togglePaid(reminder: BillReminder) {
        viewModelScope.launch {
            val result = transactionRepository.updateBillReminder(reminder.copy(isPaid = !reminder.isPaid))
            if (result is com.example.moneyby.util.Result.Error) {
                _event.value = BillReminderEvent.Error(result.message)
            }
        }
    }

    fun deleteReminder(reminder: BillReminder) {
        viewModelScope.launch {
            val result = transactionRepository.deleteBillReminder(reminder)
            if (result is com.example.moneyby.util.Result.Error) {
                _event.value = BillReminderEvent.Error(result.message)
            }
        }
    }
}

sealed class BillReminderEvent {
    data class Error(val message: String) : BillReminderEvent()
}
