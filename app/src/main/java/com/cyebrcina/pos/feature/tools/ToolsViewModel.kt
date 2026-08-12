package com.cyebrcina.pos.feature.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyebrcina.pos.data.remote.model.MenuCategory
import com.cyebrcina.pos.data.remote.realtime.FireHutRealtimeManager
import com.cyebrcina.pos.data.repository.MenuRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ToolsUiState(
    val categories: List<MenuCategory> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val togglingProductIds: Set<String> = emptySet(),
    val errorMessage: String? = null,
) {
    val visibleCategories: List<MenuCategory>
        get() {
            if (searchQuery.isBlank()) return categories
            return categories.mapNotNull { category ->
                val matches = category.products.filter { it.name.contains(searchQuery, ignoreCase = true) }
                if (matches.isEmpty()) null else category.copy(products = matches)
            }
        }
}

/** The till's "86 Board" — mark items sold out / bring them back, live-synced with Admin's own
 * Menu page and every other till via the `menu_updated` socket event. */
@HiltViewModel
class ToolsViewModel @Inject constructor(
    private val menuRepository: MenuRepository,
    realtimeManager: FireHutRealtimeManager,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val isLoading = MutableStateFlow(true)
    private val togglingProductIds = MutableStateFlow<Set<String>>(emptySet())
    private val errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ToolsUiState> = combine(
        menuRepository.categories,
        searchQuery,
        isLoading,
        togglingProductIds,
        errorMessage,
    ) { categories, query, loading, toggling, error ->
        ToolsUiState(categories, query, loading, toggling, error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ToolsUiState())

    init {
        refresh()
        realtimeManager.menuEvents.onEach { refresh() }.launchIn(viewModelScope)
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

    fun toggleSoldOut(productId: String) {
        if (productId in togglingProductIds.value) return
        viewModelScope.launch {
            togglingProductIds.update { it + productId }
            menuRepository.toggleSoldOut(productId)
                .onFailure { err -> errorMessage.value = err.message }
            togglingProductIds.update { it - productId }
        }
    }
}
