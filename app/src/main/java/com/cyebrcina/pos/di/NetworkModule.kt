package com.cyebrcina.pos.di

import com.cyebrcina.pos.BuildConfig
import com.cyebrcina.pos.data.local.DeviceSessionStore
import com.cyebrcina.pos.data.local.SessionExpiryNotifier
import com.cyebrcina.pos.data.remote.FireHutDeviceApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** Wires up the Fire Hut device/EPOS API client (see openapi.yaml, data/remote/). */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        sessionStore: DeviceSessionStore,
        sessionExpiryNotifier: SessionExpiryNotifier,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val token = sessionStore.currentTokenBlocking()
            val request = if (token != null) {
                chain.request().newBuilder().addHeader("Authorization", "Bearer $token").build()
            } else {
                chain.request()
            }
            val response = chain.proceed(request)
            if (response.code == 401) {
                sessionExpiryNotifier.notifyExpired()
            }
            response
        }
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            }
        }
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.DEVICE_API_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideFireHutDeviceApi(retrofit: Retrofit): FireHutDeviceApi = retrofit.create(FireHutDeviceApi::class.java)
}
