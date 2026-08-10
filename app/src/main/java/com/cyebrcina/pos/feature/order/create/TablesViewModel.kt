package com.cyebrcina.pos.feature.order.create

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class TablesViewModel @Inject constructor(
    private val heldOrdersStore: HeldOrdersStore,
    private val entryCoordinator: NewOrderEntryCoordinator,
) : ViewModel() {

    val heldOrders: StateFlow<List<HeldOrder>> = heldOrdersStore.heldOrders

    /** Tap on an available table — seeds the New Order flow to start there, skipping the "Create New Order" popup. */
    fun startAtTable(table: String) {
        entryCoordinator.setPending(NewOrderEntryIntent.StartAtTable(table))
    }

    /** Tap on a held order (table-bound or a Collection order surfaced in the held list) — resumes its cart. */
    fun resumeHeldOrder(heldOrderId: String) {
        entryCoordinator.setPending(NewOrderEntryIntent.ResumeHeldOrder(heldOrderId))
    }
}
