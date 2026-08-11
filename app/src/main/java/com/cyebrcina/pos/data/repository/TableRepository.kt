package com.cyebrcina.pos.data.repository

import com.cyebrcina.pos.data.remote.model.RestaurantTableDto

interface TableRepository {
    suspend fun getTables(): Result<List<RestaurantTableDto>>
}
