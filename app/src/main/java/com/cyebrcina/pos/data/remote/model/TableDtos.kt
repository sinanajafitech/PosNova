package com.cyebrcina.pos.data.remote.model

import kotlinx.serialization.Serializable

/** OCCUPIED is derived server-side from real open orders and can never be
 * hand-overridden while a table is busy — see Admin's dine-in/floor-plan
 * page, which computes the exact same rule this DTO already reflects. */
@Serializable
enum class RestaurantTableStatus { AVAILABLE, RESERVED, CLEANING, OUT_OF_SERVICE, OCCUPIED }

@Serializable
data class RestaurantTableDto(
    val id: String,
    val number: String,
    val seats: Int,
    val status: RestaurantTableStatus,
)

@Serializable
data class TablesResponse(
    val tables: List<RestaurantTableDto> = emptyList(),
)
