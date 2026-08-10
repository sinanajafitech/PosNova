package com.cyebrcina.pos.data.repository.firehut

import com.cyebrcina.pos.data.remote.FireHutDeviceApi
import com.cyebrcina.pos.data.remote.errorMessageOrDefault
import com.cyebrcina.pos.data.remote.model.SetStoreStatusRequest
import com.cyebrcina.pos.data.remote.realtime.FireHutRealtimeManager
import com.cyebrcina.pos.data.repository.StoreStatusRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.json.Json

@Singleton
class FireHutStoreStatusRepositoryImpl @Inject constructor(
    private val api: FireHutDeviceApi,
    private val realtimeManager: FireHutRealtimeManager,
    private val json: Json,
) : StoreStatusRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _acceptingOrders = MutableStateFlow<Boolean?>(null)
    override val acceptingOrders: StateFlow<Boolean?> = _acceptingOrders

    init {
        realtimeManager.storeStatusEvents.onEach { refresh() }.launchIn(scope)
    }

    override suspend fun refresh(): Result<Unit> = runCatching {
        val response = api.getStoreStatus()
        if (!response.isSuccessful) throw IllegalStateException(response.errorMessageOrDefault(json))
        _acceptingOrders.value = response.body()?.acceptingOrders
    }.recoverCatching { throw mapNetworkError(it) }

    override suspend fun setAcceptingOrders(accepting: Boolean): Result<Unit> = runCatching {
        val response = api.setStoreStatus(SetStoreStatusRequest(accepting))
        if (!response.isSuccessful) throw IllegalStateException(response.errorMessageOrDefault(json))
        _acceptingOrders.value = response.body()?.acceptingOrders ?: accepting
    }.recoverCatching { throw mapNetworkError(it) }

    private fun mapNetworkError(cause: Throwable): Throwable =
        if (cause is IOException) IOException("Couldn't reach the server — check your connection", cause) else cause
}
