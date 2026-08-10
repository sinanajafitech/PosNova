package com.cyebrcina.pos.di

import com.cyebrcina.pos.printer.PrinterService
import com.cyebrcina.pos.printer.imin.IminPrinterService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides the real printer implementation. 
 * Even in debug builds, we want to use IminPrinterService to test real Bluetooth/USB devices.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PrinterModule {

    @Binds
    @Singleton
    abstract fun bindPrinterService(
        impl: IminPrinterService
    ): PrinterService
}
