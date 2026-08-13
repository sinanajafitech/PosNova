package com.cyebrcina.pos.data.repository.firehut

import com.cyebrcina.pos.data.remote.FireHutDeviceApi
import com.cyebrcina.pos.data.remote.errorMessageOrDefault
import com.cyebrcina.pos.data.remote.model.MenuAddOn
import com.cyebrcina.pos.data.remote.model.MenuCategory
import com.cyebrcina.pos.data.remote.model.MenuModifierGroup
import com.cyebrcina.pos.data.repository.MenuRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json

@Singleton
class FireHutMenuRepositoryImpl @Inject constructor(
    private val api: FireHutDeviceApi,
    private val json: Json,
) : MenuRepository {

    private val _categories = MutableStateFlow<List<MenuCategory>>(emptyList())
    override val categories: StateFlow<List<MenuCategory>> = _categories

    private val _addOns = MutableStateFlow<List<MenuAddOn>>(emptyList())
    override val addOns: StateFlow<List<MenuAddOn>> = _addOns

    private val _modifierGroups = MutableStateFlow<List<MenuModifierGroup>>(emptyList())
    override val modifierGroups: StateFlow<List<MenuModifierGroup>> = _modifierGroups

    override suspend fun refresh(): Result<Unit> = runCatching {
        val response = api.menu()
        if (!response.isSuccessful) throw IllegalStateException(response.errorMessageOrDefault(json))
        val body = response.body() ?: throw IllegalStateException("Empty response")
        _categories.value = body.categories
        _addOns.value = body.addOns
        _modifierGroups.value = body.modifierGroups
    }.recoverCatching { cause ->
        throw if (cause is IOException) IOException("Couldn't reach the server — check your connection", cause) else cause
    }

    override suspend fun toggleSoldOut(productId: String): Result<Boolean> = runCatching {
        val response = api.toggleProductSoldOut(productId)
        if (!response.isSuccessful) throw IllegalStateException(response.errorMessageOrDefault(json))
        val available = response.body()?.available ?: throw IllegalStateException("Empty response")
        refresh()
        available
    }.recoverCatching { cause ->
        throw if (cause is IOException) IOException("Couldn't reach the server — check your connection", cause) else cause
    }
}
