package com.cyebrcina.pos.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class SavePhoneContactRequest(val phone: String, val name: String, val address: String, val notes: String)

@Serializable
data class SavePhoneContactResponse(val ok: Boolean = false, val id: String? = null, val name: String? = null)
