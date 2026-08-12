package com.cyebrcina.pos.data.repository

interface PhoneContactRepository {
    suspend fun saveContact(phone: String, name: String, address: String, notes: String): Result<Unit>
}
