package com.cyebrcina.pos.di

import com.cyebrcina.pos.payment.PaymentTerminalService
import com.cyebrcina.pos.payment.mock.MockPaymentTerminalService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds the active [PaymentTerminalService]. [MockPaymentTerminalService] by default — no
 * merchant account or SDK required, so the payment flow is fully testable. Once a provider is
 * set up (CARD_PAYMENT_SETUP.md), swap the bound type below to
 * `com.cyebrcina.pos.payment.stripe.StripeTerminalPaymentService`,
 * `com.cyebrcina.pos.payment.sumup.SumUpPaymentService`, or
 * `com.cyebrcina.pos.payment.flatpay.FlatpayPaymentService`.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PaymentModule {

    @Binds
    @Singleton
    abstract fun bindPaymentTerminalService(impl: MockPaymentTerminalService): PaymentTerminalService
}
