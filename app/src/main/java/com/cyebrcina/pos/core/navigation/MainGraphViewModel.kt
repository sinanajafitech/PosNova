package com.cyebrcina.pos.core.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyebrcina.pos.data.repository.OrderRepository
import com.cyebrcina.pos.feature.FeatureKeys
import com.cyebrcina.pos.feature.isFeatureEnabled
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Which [MainSection]s the nav rail/bottom bar actually shows — driven by Admin's Settings ->
 * Feature Management (see [OrderRepository.features]), so a disabled feature's tab disappears
 * from the till without a redeploy. [MainScaffold] itself has no repository access, so this
 * stays a small dedicated ViewModel rather than growing NewOrderViewModel or another unrelated
 * one to cover it. */
@HiltViewModel
class MainGraphViewModel @Inject constructor(
    orderRepository: OrderRepository,
) : ViewModel() {
    val visibleSections: StateFlow<List<MainSection>> = orderRepository.features
        .map { features ->
            MainSection.entries.filter { section ->
                when (section) {
                    MainSection.CALLS -> features.isFeatureEnabled(FeatureKeys.CALLS)
                    else -> true
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, MainSection.entries)
}
