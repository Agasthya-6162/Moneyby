package com.example.moneyby.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneyby.data.Category
import com.example.moneyby.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CategoryViewModel(private val repository: TransactionRepository) : ViewModel() {

    private val _event = MutableStateFlow<CategoryEvent?>(null)
    val event: StateFlow<CategoryEvent?> = _event.asStateFlow()

    fun consumeEvent() { _event.value = null }

    val categoriesState: StateFlow<List<Category>> = repository.getAllCategoriesStream()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun addCategory(name: String, color: Int, type: String, iconName: String = "category") {
        viewModelScope.launch {
            val result = repository.insertCategory(Category(name = name, color = color, type = type, iconName = iconName))
            if (result is com.example.moneyby.util.Result.Success) {
                _event.value = CategoryEvent.ShowMessage("Category added!")
            } else if (result is com.example.moneyby.util.Result.Error) {
                _event.value = CategoryEvent.ShowError(result.message)
            }
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            val result = repository.updateCategory(category)
            if (result is com.example.moneyby.util.Result.Success) {
                _event.value = CategoryEvent.ShowMessage("Category updated!")
            } else if (result is com.example.moneyby.util.Result.Error) {
                _event.value = CategoryEvent.ShowError(result.message)
            }
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            val result = repository.deleteCategory(category)
            if (result is com.example.moneyby.util.Result.Success) {
                _event.value = CategoryEvent.ShowMessage("Category deleted!")
            } else if (result is com.example.moneyby.util.Result.Error) {
                _event.value = CategoryEvent.ShowError(result.message)
            }
        }
    }
}

sealed class CategoryEvent {
    data class ShowMessage(val message: String) : CategoryEvent()
    data class ShowError(val message: String) : CategoryEvent()
}
