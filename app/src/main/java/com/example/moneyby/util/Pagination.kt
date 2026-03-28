package com.example.moneyby.util

import androidx.compose.foundation.lazy.LazyListState

/**
 * Represents a paginated list of items.
 */
data class PaginatedList<T>(
    val items: List<T>,
    val page: Int = 1,
    val pageSize: Int = 20,
    val totalItems: Int = 0,
    val hasMore: Boolean = (page * pageSize) < totalItems,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val totalPages: Int
        get() = (totalItems + pageSize - 1) / pageSize

    val isEmpty: Boolean
        get() = items.isEmpty() && !isLoading

    val isNotEmpty: Boolean
        get() = items.isNotEmpty()

    fun isListEmpty(): Boolean = items.isEmpty()

    fun nextPage(): PaginatedList<T> {
        return copy(page = page + 1, isLoading = true, error = null)
    }

    fun refresh(): PaginatedList<T> {
        return copy(page = 1, isLoading = true, error = null)
    }

    fun addMore(newItems: List<T>, newTotal: Int): PaginatedList<T> {
        return copy(
            items = items + newItems,
            totalItems = newTotal,
            isLoading = false,
            error = null
        )
    }

    fun setError(errorMessage: String): PaginatedList<T> {
        return copy(isLoading = false, error = errorMessage)
    }

    fun setLoading(): PaginatedList<T> {
        return copy(isLoading = true, error = null)
    }
}

/**
 * Pagination parameters for queries.
 */
data class PaginationParams(
    val page: Int = 1,
    val pageSize: Int = 20,
    val sortBy: String = "date",
    val sortOrder: SortOrder = SortOrder.DESC
) {
    val offset: Int
        get() = (page - 1) * pageSize

    enum class SortOrder {
        ASC, DESC
    }
}

/**
 * Lazy list scroll state extension for pagination.
 */
fun LazyListState.isScrolledToEnd(): Boolean {
    return layoutInfo.visibleItemsInfo.lastOrNull()?.index == layoutInfo.totalItemsCount - 1
}

fun LazyListState.isNearEnd(threshold: Int = 5): Boolean {
    return layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 >= layoutInfo.totalItemsCount - threshold
}

/**
 * Calculate if should load next page.
 */
fun <T> PaginatedList<T>.shouldLoadNextPage(isNearEnd: Boolean): Boolean {
    return isNearEnd && hasMore && !isLoading && error == null
}
