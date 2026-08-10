package com.cyebrcina.pos.data.repository

import com.cyebrcina.pos.data.model.DeviceSession
import kotlinx.coroutines.flow.Flow

/**
 * One shared device credential per till (Admin → Settings → Devices issues username/password),
 * not a per-cashier account — see openapi.yaml's Authentication section.
 */
interface AuthRepository {
    val session: Flow<DeviceSession?>

    suspend fun login(username: String, password: String): Result<Unit>
    suspend fun logout()
}
