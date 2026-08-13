package com.cyebrcina.pos.feature.order.create

import javax.inject.Inject
import javax.inject.Singleton

/** What [NewOrderViewModel] should do the moment it's created, set by whatever screen navigated into the New Order flow. */
sealed interface NewOrderEntryIntent {
    data class StartAtTable(val table: String) : NewOrderEntryIntent
    data class ResumeHeldOrder(val heldOrderId: String) : NewOrderEntryIntent
    /** "Take Order" tapped on the Incoming Call popup — see [com.cyebrcina.pos.feature.incomingcall.IncomingCallViewModel]. */
    data class StartFromCall(val callerName: String?, val phone: String, val callLogId: String?) : NewOrderEntryIntent

    /** "Repeat Last Order" tapped on the Incoming Call popup — same as [StartFromCall] but also
     * pre-fills the cart from [orderId]'s items (fetched fresh via OrderRepository.repeatOrder,
     * matched against the currently loaded menu — see NewOrderViewModel.startOrderFromCall). */
    data class RepeatOrderFromCall(val callerName: String?, val phone: String, val callLogId: String?, val orderId: String) : NewOrderEntryIntent
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
