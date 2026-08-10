package com.cyebrcina.pos.feature.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyebrcina.pos.data.remote.model.MenuAddOn
import com.cyebrcina.pos.data.remote.model.MenuCategory
import com.cyebrcina.pos.data.repository.MenuRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MenuUiState(
    val categories: List<MenuCategory> = emptyList(),
    val addOns: List<MenuAddOn> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
) {
    /** Categories filtered down to matching products only; categories with no matches drop out. */
    val visibleCategories: List<MenuCategory>
        get() {
            if (searchQuery.isBlank()) return categories
            return categories.mapNotNull { category ->
                val matches = category.products.filter { it.name.contains(searchQuery, ignoreCase = true) }
                if (matches.isEmpty()) null else category.copy(products = matches)
            }
        }
}

@HiltViewModel
class MenuViewModel @Inject constructor(
    private val menuRepository: MenuRepository,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val isLoading = MutableStateFlow(true)
    private val errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<MenuUiState> = combine(
        menuRepository.categories,
        menuRepository.addOns,
        searchQuery,
        isLoading,
        errorMessage,
    ) { categories, addOns, query, loading, error ->
        MenuUiState(categories, addOns, query, loading, error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MenuUiState())

    init {
        refresh()
    }

    fun onSearchQueryChange(value: String) = searchQuery.update { value }

    fun refresh() {
        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null
            menuRepository.refresh()
                .onSuccess { isLoading.value = false }
                .onFailure { err ->
                    isLoading.value = false
                    errorMessage.value = err.message
                }
        }
    }
}
