package com.cyebrcina.pos.feature.order.create

import javax.inject.Inject
import javax.inject.Singleton

/** What [NewOrderViewModel] should do the moment it's created, set by whatever screen navigated into the New Order flow. */
sealed interface NewOrderEntryIntent {
    data class StartAtTable(val table: String) : NewOrderEntryIntent
    data class ResumeHeldOrder(val heldOrderId: String) : NewOrderEntryIntent
}

/**
 * Hands a one-shot [NewOrderEntryIntent] from [TablesScreen] to the next [NewOrderViewModel]
 * instance. A plain singleton rather than nav-graph arguments — the New Order flow's shared
 * ViewModel is already scoped to the nested nav graph's own back-stack entry (see
 * `MainNavGraph.kt`), and that entry's route carries no arguments, so this is the simplest way to
 * seed a fresh instance without restructuring the graph's route pattern.
 */
@Singleton
class NewOrderEntryCoordinator @Inject constructor() {
    private var pending: NewOrderEntryIntent? = null

    fun setPending(intent: NewOrderEntryIntent) {
        pending = intent
    }

    fun consumePending(): NewOrderEntryIntent? = pending.also { pending = null }
}
