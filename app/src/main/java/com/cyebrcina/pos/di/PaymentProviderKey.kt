package com.cyebrcina.pos.di

import com.cyebrcina.pos.payment.model.PaymentProvider
import dagger.MapKey

/** Dagger has no built-in enum map key (unlike `@StringKey`/`@ClassKey`/`@IntKey`) — this is
 * what keys the `Map<PaymentProvider, PaymentTerminalService>` multibinding in [PaymentModule]. */
@MapKey
annotation class PaymentProviderKey(val value: PaymentProvider)
