package com.cyebrcina.pos.di

import com.cyebrcina.pos.payment.PaymentTerminalService
import com.cyebrcina.pos.payment.SelectedPaymentTerminalService
import com.cyebrcina.pos.payment.dojo.DojoPaymentService
import com.cyebrcina.pos.payment.flatpay.FlatpayPaymentService
import com.cyebrcina.pos.payment.mock.MockPaymentTerminalService
import com.cyebrcina.pos.payment.model.PaymentProvider
import com.cyebrcina.pos.payment.stripe.StripeTerminalPaymentService
import com.cyebrcina.pos.payment.sumup.SumUpPaymentService
import com.cyebrcina.pos.payment.teya.TeyaPaymentService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Singleton

/**
 * Binds every [PaymentTerminalService] implementation into a `Map<PaymentProvider, ...>`, and
 * separately binds [SelectedPaymentTerminalService] (which reads that map + the persisted
 * Settings choice) as the actual [PaymentTerminalService] every screen injects — so switching
 * the active provider is a Settings action, not a rebuild. Add a new provider by writing its
 * class (see [FlatpayPaymentService]'s doc for the "honest scaffold" pattern for a provider
 * without verified SDK details yet) and adding one `@Binds @IntoMap` method for it here.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PaymentModule {

    @Binds
    @IntoMap
    @PaymentProviderKey(PaymentProvider.MOCK)
    abstract fun bindMock(impl: MockPaymentTerminalService): PaymentTerminalService

    @Binds
    @IntoMap
    @PaymentProviderKey(PaymentProvider.STRIPE_TERMINAL)
    abstract fun bindStripeTerminal(impl: StripeTerminalPaymentService): PaymentTerminalService

    @Binds
    @IntoMap
    @PaymentProviderKey(PaymentProvider.SUMUP)
    abstract fun bindSumUp(impl: SumUpPaymentService): PaymentTerminalService

    @Binds
    @IntoMap
    @PaymentProviderKey(PaymentProvider.FLATPAY)
    abstract fun bindFlatpay(impl: FlatpayPaymentService): PaymentTerminalService

    @Binds
    @IntoMap
    @PaymentProviderKey(PaymentProvider.DOJO)
    abstract fun bindDojo(impl: DojoPaymentService): PaymentTerminalService

    @Binds
    @IntoMap
    @PaymentProviderKey(PaymentProvider.TEYA)
    abstract fun bindTeya(impl: TeyaPaymentService): PaymentTerminalService

    @Binds
    @Singleton
    abstract fun bindPaymentTerminalService(impl: SelectedPaymentTerminalService): PaymentTerminalService
}
