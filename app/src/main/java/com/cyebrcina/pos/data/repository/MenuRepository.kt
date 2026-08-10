package com.cyebrcina.pos.data.repository

import com.cyebrcina.pos.data.remote.model.MenuAddOn
import com.cyebrcina.pos.data.remote.model.MenuCategory
import kotlinx.coroutines.flow.StateFlow

/**
 * Read-only food catalog (GET /api/device/menu) — for staff to look items/prices up, not to
 * build an order from (Fire Hut orders still arrive pre-built from the website/other sources;
 * see openapi.yaml's Menu / food items section).
 */
interface MenuRepository {
    val categories: StateFlow<List<MenuCategory>>
    val addOns: StateFlow<List<MenuAddOn>>

    suspend fun refresh(): Result<Unit>
}
