package com.cyebrcina.pos.di

import com.cyebrcina.pos.data.repository.AuthRepository
import com.cyebrcina.pos.data.repository.MenuRepository
import com.cyebrcina.pos.data.repository.OrderRepository
import com.cyebrcina.pos.data.repository.PhoneContactRepository
import com.cyebrcina.pos.data.repository.ReportRepository
import com.cyebrcina.pos.data.repository.StoreStatusRepository
import com.cyebrcina.pos.data.repository.TableRepository
import com.cyebrcina.pos.data.repository.firehut.FireHutAuthRepositoryImpl
import com.cyebrcina.pos.data.repository.firehut.FireHutMenuRepositoryImpl
import com.cyebrcina.pos.data.repository.firehut.FireHutOrderRepositoryImpl
import com.cyebrcina.pos.data.repository.firehut.FireHutPhoneContactRepositoryImpl
import com.cyebrcina.pos.data.repository.firehut.FireHutReportRepositoryImpl
import com.cyebrcina.pos.data.repository.firehut.FireHutStoreStatusRepositoryImpl
import com.cyebrcina.pos.data.repository.firehut.FireHutTableRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds the repository interfaces to their real Fire Hut Device API implementations (openapi.yaml). */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: FireHutAuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindOrderRepository(impl: FireHutOrderRepositoryImpl): OrderRepository

    @Binds
    @Singleton
    abstract fun bindStoreStatusRepository(impl: FireHutStoreStatusRepositoryImpl): StoreStatusRepository

    @Binds
    @Singleton
    abstract fun bindReportRepository(impl: FireHutReportRepositoryImpl): ReportRepository

    @Binds
    @Singleton
    abstract fun bindMenuRepository(impl: FireHutMenuRepositoryImpl): MenuRepository

    @Binds
    @Singleton
    abstract fun bindTableRepository(impl: FireHutTableRepositoryImpl): TableRepository

    @Binds
    @Singleton
    abstract fun bindPhoneContactRepository(impl: FireHutPhoneContactRepositoryImpl): PhoneContactRepository
}
