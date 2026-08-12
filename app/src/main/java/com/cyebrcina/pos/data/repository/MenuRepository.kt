package com.cyebrcina.pos.data.repository

import com.cyebrcina.pos.data.remote.model.MenuAddOn
import com.cyebrcina.pos.data.remote.model.MenuCategory
import kotlinx.coroutines.flow.StateFlow

/**
 * The food catalog (GET /api/device/menu) — for staff to look items/prices up, not to build an
 * order from (Fire Hut orders still arrive pre-built from the website/other sources; see
 * openapi.yaml's Menu / food items section). [toggleSoldOut] is the one write path: the till's
 * Tools/86 Board.
 */
interface MenuRepository {
    val categories: StateFlow<List<MenuCategory>>
    val addOns: StateFlow<List<MenuAddOn>>

    suspend fun refresh(): Result<Unit>

    /** 86s [productId], or brings it back if it's already 86'd. Refreshes [categories] on
     * success so the new availability shows immediately, not just on the next poll/socket event. */
    suspend fun toggleSoldOut(productId: String): Result<Boolean>
}
